package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.AnalysisProfilePointDto;
import demo.chess.api.dto.AnalysisReplaySettingsDto;
import demo.chess.api.dto.AnalysisReplayStepDto;
import demo.chess.api.dto.BoardDto;
import demo.chess.api.dto.EngineLineDto;
import demo.chess.definitions.engines.DeepAnalysisEngine;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.impl.DeepAnalysisUciEngine;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.game.TerminalPositionEvaluator;
import demo.chess.game.impl.Simulation;
import demo.chess.notation.PgnNotation;

@Service
public class AnalysisReplayService {

    private final GameService gameService;
    private final EngineSettingsService engineSettingsService;
    private final EvaluationService evaluationService;
    private final UciGameService uciGameService;
    private final EngineLineDisplayService engineLineDisplayService;
    private AnalysisReplaySession session;

    /**
     * Creates a new AnalysisReplayService instance.
     * @param gameService the game service
     * @param engineSettingsService the engine settings service
     * @param evaluationService the evaluation service
     * @param uciGameService the uci game service
     * @param engineLineDisplayService canonical engine-line display converter
     */
    public AnalysisReplayService(
            GameService gameService,
            EngineSettingsService engineSettingsService,
            EvaluationService evaluationService,
            UciGameService uciGameService,
            EngineLineDisplayService engineLineDisplayService) {
        this.gameService = gameService;
        this.engineSettingsService = engineSettingsService;
        this.evaluationService = evaluationService;
        this.uciGameService = uciGameService;
        this.engineLineDisplayService = engineLineDisplayService;
    }

    /**
     * Performs the start operation.
     * @param settings the settings
     * @return the result of the operation
     */
    public synchronized AnalysisReplayStepDto start(AnalysisReplaySettingsDto settings)
            throws NoMoveFoundException, IOException {
        closeSessionEngine();
        evaluationService.stopLiveEvaluation();

        List<Move> moveListSnapshot = uciGameService.getAnalysisMoveListSnapshot();
        String requestedProfileId = settings != null ? settings.getEngineProfileId() : null;
        int depth = settings != null ? Math.max(0, settings.getDepth()) : 0;
        int moveTimeSeconds = settings != null ? Math.max(1, settings.getMoveTimeSeconds()) : 5;
        String engineProfileId = engineSettingsService.normalizeDeepAnalysisProfileId(requestedProfileId);
        UciEngineConfig engineConfig = engineSettingsService.getDeepAnalysisConfig(
                engineProfileId,
                depth,
                moveTimeSeconds);
        DeepAnalysisEngine deepAnalysisEngine = createDeepAnalysisEngine(engineConfig.getEngine());
        String engineName = engineConfig.getEngineName();

        AnalysisReplaySession newSession = new AnalysisReplaySession(
                moveListSnapshot,
                Simulation.createSimulation(),
                deepAnalysisEngine,
                engineConfig,
                engineName);

        double initialEvaluation = 0.3;
        newSession.profile.add(new AnalysisProfilePointDto(
                0,
                null,
                null,
                "Start",
                initialEvaluation,
                EvaluationBarMapper.toBar(initialEvaluation),
                0));

        this.session = newSession;
        return toStepDto(newSession, false, null, null, null, 0.0, 0.5, 0, "Analysis replay started.");
    }

    /**
     * Performs the next operation.
     * @return the result of the operation
     */
    public synchronized AnalysisReplayStepDto next() throws NoMoveFoundException, IOException {
        if (session == null || !session.active) {
            return inactiveStep("No active analysis replay.");
        }

        if (session.currentPly >= session.originalMoves.size()) {
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
                    "Analysis replay finished.");
        }

        Move originalMove = session.originalMoves.get(session.currentPly);
        Move replayMove = session.replayGame.getPlayer().getMoveInSimulation(session.replayGame, originalMove);
        if (replayMove == null) {
            throw new NoMoveFoundException("Could not map analysis replay move: " + originalMove);
        }

        String from = replayMove.getSource() != null ? replayMove.getSource().getName() : null;
        String to = replayMove.getTarget() != null ? replayMove.getTarget().getName() : null;
        String san = PgnNotation.toDisplayNotationAndApply(session.replayGame, replayMove);
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


    /**
     * Cancels the current analysis replay.
     * @return current replay state
     */
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

    private DeepAnalysisEngine createDeepAnalysisEngine(String enginePath) {
        String effectivePath = enginePath == null || enginePath.isBlank()
                ? engineSettingsService.getDefaultEnginePath()
                : enginePath.trim();
        try {
            DeepAnalysisUciEngine engine = new DeepAnalysisUciEngine(effectivePath);
            engine.setManagementLabel("deep analysis");
            return engine;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not start deep analysis engine at " + effectivePath, ex);
        }
    }

    private AnalysisEvaluation analyzeCurrentReplayPosition(AnalysisReplaySession source)
            throws NoMoveFoundException {
        AnalysisEvaluation terminalEvaluation = evaluateTerminalPosition(source);
        if (terminalEvaluation != null) {
            return terminalEvaluation;
        }

        try {
            source.engine.clearChachedLines();
            List<EngineLine> bestLines = source.engine.getBestLines(
                    source.replayGame,
                    source.engineConfig);

            if (bestLines == null || bestLines.isEmpty()) {
                return new AnalysisEvaluation(0.0, 0.5, 0, List.of());
            }

            double eval = bestLines.get(0).getEvaluation();
            double bar = EvaluationBarMapper.toBar(eval);
            int depth = bestLines.get(0).getDepth();

            List<EngineLineDto> lines = new ArrayList<>();
            for (EngineLine line : bestLines) {
                Game displayGame = Simulation.forkDummyFrom(source.replayGame.getMoveList());
                lines.add(engineLineDisplayService.toDto(displayGame, line));
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

        Double evaluation = TerminalEvaluationMapper.toEvaluation(
                TerminalPositionEvaluator.determineState(source.replayGame));
        if (evaluation == null) {
            return null;
        }

        return new AnalysisEvaluation(
                evaluation,
                EvaluationBarMapper.toBar(evaluation),
                latestDepth(source),
                List.of());
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
                source.engineName,
                board,
                new ArrayList<>(source.profile),
                message);
    }

    private void closeSessionEngine() {
        if (session == null || session.engine == null) {
            return;
        }
        safeStop(session.engine);
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
        private final String engineName;
        private final List<AnalysisProfilePointDto> profile = new ArrayList<>();
        private int currentPly = 0;
        private boolean active = true;

        private AnalysisReplaySession(
                List<Move> originalMoves,
                Game replayGame,
                DeepAnalysisEngine engine,
                UciEngineConfig engineConfig,
                String engineName) {
            this.originalMoves = originalMoves;
            this.replayGame = replayGame;
            this.engine = engine;
            this.engineConfig = engineConfig;
            this.engineName = engineName;
        }
    }
}
