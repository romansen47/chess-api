package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import demo.chess.api.dto.AnalysisProfilePointDto;
import demo.chess.api.dto.AnalysisReplaySettingsDto;
import demo.chess.api.dto.AnalysisReplayStepDto;
import demo.chess.api.dto.BoardDto;
import demo.chess.api.dto.EngineLineDto;
import demo.chess.definitions.engines.DeepAnalysisEngine;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.impl.DeepAnalysisUciEngine;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.players.Player;
import demo.chess.definitions.states.State;
import demo.chess.game.Game;
import demo.chess.game.impl.Simulation;

@Service
public class AnalysisReplayService {

    private final GameService gameService;
    private final EngineSettingsService engineSettingsService;
    private AnalysisReplaySession session;

    public AnalysisReplayService(GameService gameService, EngineSettingsService engineSettingsService) {
        this.gameService = gameService;
        this.engineSettingsService = engineSettingsService;
    }

    public synchronized AnalysisReplayStepDto start(AnalysisReplaySettingsDto settings)
            throws NoMoveFoundException, IOException {
        closeSessionEngine();

        List<Move> moveListSnapshot = gameService.getMoveListSnapshot();
        AnalysisReplaySettingsDto normalizedSettings = normalizeSettings(settings);
        UciEngineConfig engineConfig = toEngineConfig(normalizedSettings);
        DeepAnalysisEngine deepAnalysisEngine = createDeepAnalysisEngine();

        AnalysisReplaySession newSession = new AnalysisReplaySession(
                moveListSnapshot,
                Simulation.createSimulation(),
                deepAnalysisEngine,
                engineConfig);

        newSession.profile.add(new AnalysisProfilePointDto(
                0,
                null,
                null,
                "Start",
                0.0,
                0.5,
                0));

        this.session = newSession;
        return toStepDto(newSession, false, null, null, null, 0.0, 0.5, 0, "Analysis replay started.");
    }

    public synchronized AnalysisReplayStepDto next() throws NoMoveFoundException, IOException {
        if (session == null || !session.active) {
            return inactiveStep("No active analysis replay.");
        }

        if (session.currentPly >= session.originalMoves.size()) {
            session.active = false;
            closeSessionEngine();
            return toStepDto(session, true, null, null, null, latestEvaluation(session), latestBar(session), latestDepth(session),
                    "Analysis replay finished.");
        }

        Move originalMove = session.originalMoves.get(session.currentPly);
        Move replayMove = session.replayGame.getPlayer().getMoveInSimulation(session.replayGame, originalMove);

        String from = replayMove.getSource() != null ? replayMove.getSource().getName() : null;
        String to = replayMove.getTarget() != null ? replayMove.getTarget().getName() : null;
        String san = originalMove.toString();

        session.replayGame.apply(replayMove);
        session.currentPly++;

        AnalysisEvaluation evaluation = analyzeCurrentReplayPosition(session);

        session.profile.add(new AnalysisProfilePointDto(
                session.currentPly,
                from,
                to,
                san,
                Math.round(evaluation.evaluation * 100.0) / 100.0,
                evaluation.bar,
                evaluation.depth,
                evaluation.lines));

        boolean done = session.currentPly >= session.originalMoves.size();
        if (done) {
            session.active = false;
            closeSessionEngine();
        }

        return toStepDto(
                session,
                done,
                from,
                to,
                san,
                evaluation.evaluation,
                evaluation.bar,
                evaluation.depth,
                done ? "Analysis replay finished." : null);
    }

    public synchronized AnalysisReplayStepDto state() {
        if (session == null) {
            return inactiveStep("No active analysis replay.");
        }

        return toStepDto(
                session,
                !session.active,
                null,
                null,
                null,
                latestEvaluation(session),
                latestBar(session),
                latestDepth(session),
                null);
    }

    public synchronized AnalysisReplayStepDto cancel() {
        if (session == null) {
            return inactiveStep("No active analysis replay.");
        }

        session.active = false;
        closeSessionEngine();
        return toStepDto(
                session,
                true,
                null,
                null,
                null,
                latestEvaluation(session),
                latestBar(session),
                latestDepth(session),
                "Analysis replay cancelled.");
    }

    private AnalysisReplaySettingsDto normalizeSettings(AnalysisReplaySettingsDto settings) {
        AnalysisReplaySettingsDto source = settings != null ? settings : new AnalysisReplaySettingsDto();

        int moveTimeSeconds = source.getMoveTimeSeconds() > 0 ? source.getMoveTimeSeconds() : 5;
        int depth = Math.max(0, source.getDepth());
        int threads = source.getThreads() > 0 ? source.getThreads() : 1;
        int hashSize = source.getHashSize() > 0 ? source.getHashSize() : 256;
        int multiPV = source.getMultiPV() > 0 ? source.getMultiPV() : 3;
        int contempt = source.getContempt();
        int uciElo = Math.max(0, source.getUciElo());

        return new AnalysisReplaySettingsDto(
                moveTimeSeconds,
                depth,
                threads,
                hashSize,
                multiPV,
                contempt,
                uciElo);
    }

    private UciEngineConfig toEngineConfig(AnalysisReplaySettingsDto settings) {
        UciEngineConfig config = new UciEngineConfig();
        config.setDepth(settings.getDepth());
        config.setThreads(settings.getThreads());
        config.setHashSize(settings.getHashSize());
        config.setMultiPV(settings.getMultiPV());
        config.setContempt(settings.getContempt());
        config.setMoveOverhead(settings.getMoveTimeSeconds());
        config.setUciElo(settings.getUciElo());
        return config;
    }

    private DeepAnalysisEngine createDeepAnalysisEngine() {
        String configuredPath = engineSettingsService.getEvaluationEnginePath();
        String defaultPath = engineSettingsService.getDefaultEnginePath();
        String effectivePath = configuredPath == null || configuredPath.isBlank()
                ? defaultPath
                : configuredPath.trim();

        try {
            return new DeepAnalysisUciEngine(effectivePath);
        } catch (Exception ex) {
            if (defaultPath.equals(effectivePath)) {
                throw new IllegalStateException("Could not start deep analysis engine at " + effectivePath, ex);
            }

            try {
                return new DeepAnalysisUciEngine(defaultPath);
            } catch (Exception fallbackEx) {
                throw new IllegalStateException("Could not start deep analysis engine at " + defaultPath, fallbackEx);
            }
        }
    }

    private AnalysisEvaluation analyzeCurrentReplayPosition(AnalysisReplaySession source) {
        AnalysisEvaluation terminalEvaluation = evaluateTerminalPosition(source);
        if (terminalEvaluation != null) {
            return terminalEvaluation;
        }

        try {
            source.engine.clearChachedLines();
            List<Pair<Pair<Double, Integer>, String>> bestLines = source.engine.getBestLines(
                    source.replayGame,
                    source.engineConfig);

            if (bestLines == null || bestLines.isEmpty()) {
                return new AnalysisEvaluation(0.0, 0.5, 0, List.of());
            }

            double eval = bestLines.get(0).getLeft().getLeft();
            double bar = mapEvalToBar(eval);
            int depth = bestLines.get(0).getLeft().getRight();

            List<EngineLineDto> lines = new ArrayList<>();
            for (Pair<Pair<Double, Integer>, String> line : bestLines) {
                double lineEval = line.getLeft().getLeft();
                int lineDepth = line.getLeft().getRight();
                String movesUci = line.getRight();
                double roundedEval = Math.round(lineEval * 100.0) / 100.0;
                lines.add(new EngineLineDto(roundedEval, lineDepth, movesUci));
            }

            return new AnalysisEvaluation(eval, bar, depth, lines);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            safeStop(source.engine);
            return new AnalysisEvaluation(0.0, 0.5, 0, List.of());
        } catch (ExecutionException | IOException e) {
            safeStop(source.engine);
            return new AnalysisEvaluation(0.0, 0.5, 0, List.of());
        }
    }

    private AnalysisEvaluation evaluateTerminalPosition(AnalysisReplaySession source) {
        if (source == null || source.replayGame == null) {
            return null;
        }

        AnalysisEvaluation explicitStateEvaluation = evaluateExplicitTerminalState(source);
        if (explicitStateEvaluation != null) {
            return explicitStateEvaluation;
        }

        return evaluateTerminalSimulationPosition(source);
    }

    private AnalysisEvaluation evaluateExplicitTerminalState(AnalysisReplaySession source) {
        State state = source.replayGame.getState();
        if (state == null) {
            return null;
        }

        if (state == State.BLACK_MATED || state == State.BLACK_RESIGNED) {
            return new AnalysisEvaluation(100.0, 1.0, latestDepth(source), List.of());
        }

        if (state == State.WHITE_MATED || state == State.WHITE_RESIGNED) {
            return new AnalysisEvaluation(-100.0, 0.0, latestDepth(source), List.of());
        }

        if (state == State.STALEMATE
                || state == State.DRAW_BY_50_MOVES_RULE
                || state == State.DRAW_BY_THREEFOLD_REPETITION) {
            return new AnalysisEvaluation(0.0, 0.5, latestDepth(source), List.of());
        }

        return null;
    }

    private AnalysisEvaluation evaluateTerminalSimulationPosition(AnalysisReplaySession source) {
        Player playerToMove = source.replayGame.getPlayer();
        if (playerToMove == null || playerToMove.getKing() == null || playerToMove.getKing().getField() == null) {
            return null;
        }

        try {
            if (!playerToMove.getValidMoves(source.replayGame).isEmpty()) {
                return null;
            }
        } catch (NoMoveFoundException | IOException e) {
            return null;
        }

        Player opponent = playerToMove == source.replayGame.getWhitePlayer()
                ? source.replayGame.getBlackPlayer()
                : source.replayGame.getWhitePlayer();

        boolean kingIsAttacked = opponent.getSimpleMoves().stream()
                .map(Move::getTarget)
                .anyMatch(playerToMove.getKing().getField()::equals);

        if (!kingIsAttacked) {
            return new AnalysisEvaluation(0.0, 0.5, latestDepth(source), List.of());
        }

        if (playerToMove == source.replayGame.getWhitePlayer()) {
            return new AnalysisEvaluation(-100.0, 0.0, latestDepth(source), List.of());
        }

        return new AnalysisEvaluation(100.0, 1.0, latestDepth(source), List.of());
    }

    private double mapEvalToBar(double eval) {
        if (eval >= 99d) {
            return 1.0;
        }
        if (eval <= -99d) {
            return 0.0;
        }
        return 0.5 + Math.atan(Math.tan(Math.PI / 10.0) * eval) / Math.PI;
    }

    private double latestEvaluation(AnalysisReplaySession source) {
        if (source == null || source.profile.isEmpty()) {
            return 0.0;
        }
        return source.profile.get(source.profile.size() - 1).getEvaluation();
    }

    private double latestBar(AnalysisReplaySession source) {
        if (source == null || source.profile.isEmpty()) {
            return 0.5;
        }
        return source.profile.get(source.profile.size() - 1).getBar();
    }

    private int latestDepth(AnalysisReplaySession source) {
        if (source == null || source.profile.isEmpty()) {
            return 0;
        }
        return source.profile.get(source.profile.size() - 1).getDepth();
    }

    private AnalysisReplayStepDto inactiveStep(String message) {
        return new AnalysisReplayStepDto(
                false,
                true,
                0,
                0,
                null,
                null,
                null,
                0.0,
                0.5,
                0,
                null,
                List.of(),
                message);
    }

    private AnalysisReplayStepDto toStepDto(
            AnalysisReplaySession source,
            boolean done,
            String from,
            String to,
            String san,
            double evaluation,
            double bar,
            int depth,
            String message) {
        BoardDto board = gameService.getBoardView(source.replayGame);
        return new AnalysisReplayStepDto(
                source.active,
                done,
                source.originalMoves.size(),
                source.currentPly,
                from,
                to,
                san,
                Math.round(evaluation * 100.0) / 100.0,
                bar,
                depth,
                board,
                new ArrayList<>(source.profile),
                message);
    }

    private void closeSessionEngine() {
        if (session == null || session.engine == null) {
            return;
        }
        safeStop(session.engine);
        try {
            session.engine.close();
        } catch (Exception ignored) {
        }
    }

    private void safeStop(DeepAnalysisEngine engine) {
        if (engine == null) {
            return;
        }
        try {
            engine.stopEvaluation();
        } catch (Exception ignored) {
        }
    }

    private static class AnalysisEvaluation {
        private final double evaluation;
        private final double bar;
        private final int depth;
        private final List<EngineLineDto> lines;

        private AnalysisEvaluation(double evaluation, double bar, int depth, List<EngineLineDto> lines) {
            this.evaluation = evaluation;
            this.bar = bar;
            this.depth = depth;
            this.lines = lines;
        }
    }

    private static class AnalysisReplaySession {
        private final List<Move> originalMoves;
        private final Game replayGame;
        private final DeepAnalysisEngine engine;
        private final UciEngineConfig engineConfig;
        private final List<AnalysisProfilePointDto> profile = new ArrayList<>();
        private int currentPly = 0;
        private boolean active = true;

        private AnalysisReplaySession(
                List<Move> originalMoves,
                Game replayGame,
                DeepAnalysisEngine engine,
                UciEngineConfig engineConfig) {
            this.originalMoves = originalMoves;
            this.replayGame = replayGame;
            this.engine = engine;
            this.engineConfig = engineConfig;
        }
    }
}
