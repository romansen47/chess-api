package demo.chess.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.AnalysisProfilePointDto;
import demo.chess.api.dto.EngineLineDto;
import demo.chess.api.dto.GameAnalysisDtos;
import demo.chess.api.dto.UciGameDto;
import demo.chess.database.AnalysisLine;
import demo.chess.database.AnalysisPosition;
import demo.chess.database.AnalysisRun;
import demo.chess.database.AnalysisRunSummary;
import demo.chess.definitions.engines.UciEngineConfig;

/** Owns persistence and deterministic comparison of completed game analyses. */
@Service
public class GameAnalysisHistoryService {

    private final ChessDatabaseService databaseService;
    private final UciGameService uciGameService;
    private final EngineSettingsService engineSettingsService;

    public GameAnalysisHistoryService(
            ChessDatabaseService databaseService,
            UciGameService uciGameService,
            EngineSettingsService engineSettingsService) {
        this.databaseService = databaseService;
        this.uciGameService = uciGameService;
        this.engineSettingsService = engineSettingsService;
    }

    /** Stores a completed run. A new run becomes active but never deletes older runs. */
    public long saveCompletedRun(
            String gameKey,
            String engineProfileId,
            String profileName,
            UciEngineConfig config,
            List<AnalysisProfilePointDto> profile) throws SQLException, IOException {
        Objects.requireNonNull(config, "config");
        AnalysisRun run = new AnalysisRun(
                0L,
                gameKey,
                config.getEngineName(),
                engineProfileId,
                profileName,
                config.getDepth(),
                config.getMoveTimeSeconds(),
                config.getIntOption("MultiPV", 1),
                config.toUciSetOptionCommands(),
                null,
                false,
                toStoredPositions(profile));
        return databaseService.saveAnalysisRun(run);
    }

    public List<GameAnalysisDtos.RunSummary> listCurrentGameRuns()
            throws SQLException, IOException {
        return databaseService.findAnalysisRuns(currentGameKey()).stream()
                .map(this::toSummary)
                .toList();
    }

    public GameAnalysisDtos.Run getRun(long runId) throws SQLException, IOException {
        AnalysisRun run = requireCurrentGameRun(runId);
        return new GameAnalysisDtos.Run(toSummary(run), toPositions(run.positions()));
    }

    public void activate(long runId) throws SQLException, IOException {
        AnalysisRun run = requireCurrentGameRun(runId);
        databaseService.setActiveAnalysisRun(run.gameKey(), runId);
    }

    public GameAnalysisDtos.Comparison compare(long leftRunId, long rightRunId)
            throws SQLException, IOException {
        if (leftRunId == rightRunId) {
            throw new IllegalArgumentException("Choose two different analysis runs");
        }
        AnalysisRun left = requireCurrentGameRun(leftRunId);
        AnalysisRun right = requireCurrentGameRun(rightRunId);
        if (!left.gameKey().equals(right.gameKey())) {
            throw new IllegalArgumentException("Analysis runs belong to different games");
        }

        Map<Integer, AnalysisPosition> rightByPly = new LinkedHashMap<>();
        right.positions().forEach(position -> rightByPly.put(position.ply(), position));
        List<GameAnalysisDtos.PositionComparison> positions = new ArrayList<>();
        for (AnalysisPosition leftPosition : left.positions()) {
            AnalysisPosition rightPosition = rightByPly.get(leftPosition.ply());
            if (rightPosition == null) continue;
            positions.add(comparePosition(leftPosition, rightPosition));
        }
        return new GameAnalysisDtos.Comparison(
                toSummary(left), toSummary(right), List.copyOf(positions));
    }

    String currentGameKey() throws IOException {
        try {
            UciGameDto game = uciGameService.getCurrentGameSnapshot().getGame();
            StringBuilder canonical = new StringBuilder()
                    .append(game.getStartingPositionId()).append(':');
            game.getMoves().forEach(move -> canonical.append(move.getUci()).append(' '));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        } catch (Exception e) {
            if (e instanceof IOException io) throw io;
            throw new IOException("Could not identify the selected game", e);
        }
    }

    private AnalysisRun requireCurrentGameRun(long runId) throws SQLException, IOException {
        AnalysisRun run = databaseService.getAnalysisRun(runId);
        if (!run.gameKey().equals(currentGameKey())) {
            throw new IllegalArgumentException("Analysis run does not belong to the selected game");
        }
        return run;
    }

    private List<AnalysisPosition> toStoredPositions(List<AnalysisProfilePointDto> profile) {
        if (profile == null) return List.of();
        return profile.stream()
                .map(point -> {
                    List<AnalysisLine> lines = new ArrayList<>();
                    List<EngineLineDto> sourceLines = point.getLines() != null
                            ? point.getLines() : List.of();
                    for (int index = 0; index < sourceLines.size(); index++) {
                        EngineLineDto line = sourceLines.get(index);
                        lines.add(new AnalysisLine(
                                index + 1, line.getEval(), line.getDepth(),
                                line.getMateDistance(), line.getUciMoves()));
                    }
                    Integer mate = sourceLines.isEmpty()
                            ? null : sourceLines.get(0).getMateDistance();
                    return new AnalysisPosition(
                            point.getPly(), point.getEvaluation(), point.getDepth(), mate, lines);
                })
                .toList();
    }

    private GameAnalysisDtos.PositionComparison comparePosition(
            AnalysisPosition left,
            AnalysisPosition right) {
        List<String> leftPv = bestPv(left);
        List<String> rightPv = bestPv(right);
        String leftBest = first(leftPv);
        String rightBest = first(rightPv);
        boolean differentBest = !Objects.equals(leftBest, rightBest);
        int commonPrefix = commonPrefix(leftPv, rightPv);
        boolean differentContinuation = !differentBest
                && (!leftPv.equals(rightPv));
        boolean mateDisagreement = (left.mateDistance() == null)
                != (right.mateDistance() == null);
        double gap = Math.round(Math.abs(
                NormalizedEvaluationScore.fromPawns(left.evaluation())
                        - NormalizedEvaluationScore.fromPawns(right.evaluation())) * 100.0) / 100.0;
        return new GameAnalysisDtos.PositionComparison(
                left.ply(), toPosition(left), toPosition(right),
                differentBest, differentContinuation, mateDisagreement, gap,
                commonPrefix, rankOf(right, leftBest), rankOf(left, rightBest));
    }

    private Integer rankOf(AnalysisPosition position, String move) {
        if (move == null) return null;
        return position.lines().stream()
                .filter(line -> move.equals(first(line.uciMoves())))
                .map(AnalysisLine::rank)
                .findFirst()
                .orElse(null);
    }

    private int commonPrefix(List<String> left, List<String> right) {
        int length = Math.min(left.size(), right.size());
        int index = 0;
        while (index < length && Objects.equals(left.get(index), right.get(index))) index++;
        return index;
    }

    private List<String> bestPv(AnalysisPosition position) {
        return position.lines().isEmpty() ? List.of() : position.lines().get(0).uciMoves();
    }

    private String first(List<String> moves) {
        return moves == null || moves.isEmpty() ? null : moves.get(0);
    }

    private GameAnalysisDtos.RunSummary toSummary(AnalysisRunSummary run) {
        return new GameAnalysisDtos.RunSummary(
                run.id(), run.engineName(), run.engineProfileId(),
                displayProfileName(run.profileName(), run.engineProfileId()), run.requestedDepth(),
                run.moveTimeSeconds(), run.multiPv(), run.positionCount(), run.maximumDepth(),
                run.createdAt(), run.active());
    }

    private GameAnalysisDtos.RunSummary toSummary(AnalysisRun run) {
        int maximumDepth = run.positions().stream()
                .mapToInt(AnalysisPosition::depth).max().orElse(0);
        return new GameAnalysisDtos.RunSummary(
                run.id(), run.engineName(), run.engineProfileId(),
                displayProfileName(run.profileName(), run.engineProfileId()), run.requestedDepth(),
                run.moveTimeSeconds(), run.multiPv(), run.positions().size(), maximumDepth,
                run.createdAt(), run.active());
    }

    /** Resolves runs created before profile names were stored without rewriting history. */
    private String displayProfileName(String storedName, String profileId) {
        if (storedName != null && !storedName.isBlank() && !storedName.equals(profileId)) {
            return storedName;
        }
        try {
            return engineSettingsService.getProfileName(profileId);
        } catch (RuntimeException ignored) {
            return storedName == null || storedName.isBlank() ? profileId : storedName;
        }
    }

    private List<GameAnalysisDtos.Position> toPositions(List<AnalysisPosition> positions) {
        return positions.stream().map(this::toPosition).toList();
    }

    private GameAnalysisDtos.Position toPosition(AnalysisPosition position) {
        return new GameAnalysisDtos.Position(
                position.ply(), position.evaluation(),
                NormalizedEvaluationScore.fromPawns(position.evaluation()),
                position.depth(), position.mateDistance(),
                position.lines().stream().map(line -> new GameAnalysisDtos.Line(
                        line.rank(), line.evaluation(),
                        NormalizedEvaluationScore.fromPawns(line.evaluation()),
                        line.depth(), line.mateDistance(), line.uciMoves())).toList());
    }
}
