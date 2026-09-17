package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import demo.chess.analysis.annotation.MoveAnnotation;
import demo.chess.analysis.annotation.MoveAnnotationClassifier;
import demo.chess.api.dto.AnalysisProfilePointDto;
import demo.chess.api.dto.AnalysisReplaySettingsDto;
import demo.chess.api.dto.AnalysisReplayStepDto;
import demo.chess.api.dto.BoardDto;
import demo.chess.api.dto.EngineLineDto;
import demo.chess.api.engine.NativeEngineRole;
import demo.chess.api.exception.NativeEngineUnavailableException;
import demo.chess.api.mapper.MoveAnnotationDtoMapper;
import demo.chess.definitions.engines.DeepAnalysisEngine;
import demo.chess.definitions.engines.DeepAnalysisResult;
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
    private final EngineAvailabilityService engineAvailabilityService;
    private final EvaluationService evaluationService;
    private final UciGameService uciGameService;
    private final EngineLineDisplayService engineLineDisplayService;
    private final MoveAnnotationClassifier moveAnnotationClassifier = new MoveAnnotationClassifier();
    private AnalysisReplaySession session;

    @Autowired
    public AnalysisReplayService(
            GameService gameService,
            EngineSettingsService engineSettingsService,
            EngineAvailabilityService engineAvailabilityService,
            EvaluationService evaluationService,
            UciGameService uciGameService,
            EngineLineDisplayService engineLineDisplayService) {
        this.gameService = gameService;
        this.engineSettingsService = engineSettingsService;
        this.engineAvailabilityService = engineAvailabilityService;
        this.evaluationService = evaluationService;
        this.uciGameService = uciGameService;
        this.engineLineDisplayService = engineLineDisplayService;
    }

    /**
     * Compatibility constructor retained for direct unit tests and embedders
     * that used the pre-capabilities service signature.
     */
    public AnalysisReplayService(
            GameService gameService,
            EngineSettingsService engineSettingsService,
            EvaluationService evaluationService,
            UciGameService uciGameService,
            EngineLineDisplayService engineLineDisplayService) {
        this(
                gameService,
                engineSettingsService,
                new EngineAvailabilityService(
                        new EngineRuntimeSelectionService(engineSettingsService),
                        engineSettingsService),
                evaluationService,
                uciGameService,
                engineLineDisplayService);
    }

    public synchronized AnalysisReplayStepDto start(AnalysisReplaySettingsDto settings)
            throws NoMoveFoundException, IOException {
        List<Move> moveListSnapshot = uciGameService.getAnalysisMoveListSnapshot();
        String requestedProfileId = settings != null ? settings.getEngineProfileId() : null;
        int depth = settings != null ? Math.max(0, settings.getDepth()) : 0;
        int moveTimeSeconds = settings != null ? Math.max(1, settings.getMoveTimeSeconds()) : 5;

        String engineProfileId = engineAvailabilityService
                .findAvailableDeepAnalysisProfileId(requestedProfileId)
                .orElseThrow(() -> new NativeEngineUnavailableException(NativeEngineRole.DEEP_ANALYSIS));
        UciEngineConfig engineConfig = engineSettingsService.getDeepAnalysisConfig(
                engineProfileId, depth, moveTimeSeconds);

        DeepAnalysisEngine deepAnalysisEngine = createDeepAnalysisEngine(engineConfig.getEngine());
        String engineName = engineConfig.getEngineName();
        AnalysisReplaySession newSession = new AnalysisReplaySession(
                moveListSnapshot,
                Simulation.createSimulation(uciGameService.getAnalysisStartingPosition()),
                deepAnalysisEngine,
                engineConfig,
                engineName);

        double initialEvaluation = 0.3;
        newSession.profile.add(new AnalysisProfilePointDto(
                0, null, null, "Start", initialEvaluation,
                EvaluationBarMapper.toBar(initialEvaluation), 0));

        closeSessionEngine();
        evaluationService.stopLiveEvaluation();
        this.session = newSession;
        return toStepDto(newSession, false, null, null, null, 0.0, 0.5, 0, "Analysis replay started.");
    }

    public synchronized AnalysisReplayStepDto next() throws NoMoveFoundException, IOException {
        if (session == null || !session.active) return inactiveStep("No active analysis replay.");
        if (session.currentPly >= session.originalMoves.size()) {
            session.active = false;
            closeSessionEngine();
            return toStepDto(session, true, null, null, null,
                    latestEvaluation(session), latestBar(session), latestDepth(session),
                    "Analysis replay finished.");
        }

        Move originalMove = session.originalMoves.get(session.currentPly);
        DeepAnalysisResult analysisBeforeMove = session.lastDeepAnalysisResult;
        Game positionBeforeMove = analysisBeforeMove != null
                ? Simulation.forkSimulationFrom(session.replayGame.getMoveList())
                : null;

        Move replayMove = session.replayGame.getPlayer().getMoveInSimulation(session.replayGame, originalMove);
        if (replayMove == null) throw new NoMoveFoundException("Could not map analysis replay move: " + originalMove);
        String from = replayMove.getSource() != null ? replayMove.getSource().getName() : null;
        String to = replayMove.getTarget() != null ? replayMove.getTarget().getName() : null;
        String san = PgnNotation.toDisplayNotationAndApply(session.replayGame, replayMove);
        session.currentPly++;

        AnalysisEvaluation evaluation = analyzeCurrentReplayPosition(session);
        MoveAnnotation annotation = null;
        if (analysisBeforeMove != null && positionBeforeMove != null) {
            annotation = moveAnnotationClassifier.classify(
                    positionBeforeMove, originalMove.toString(), analysisBeforeMove, evaluation.evaluation);
        }

        AnalysisProfilePointDto profilePoint = new AnalysisProfilePointDto(
                session.currentPly, from, to, san,
                Math.round(evaluation.evaluation * 100.0) / 100.0,
                evaluation.bar, evaluation.depth, evaluation.lines);
        profilePoint.setAnnotation(MoveAnnotationDtoMapper.toDto(annotation));
        session.profile.add(profilePoint);
        session.lastDeepAnalysisResult = evaluation.deepAnalysisResult;

        boolean done = session.currentPly >= session.originalMoves.size();
        if (done) {
            session.active = false;
            closeSessionEngine();
        }
        return toStepDto(session, done, from, to, san,
                evaluation.evaluation, evaluation.bar, evaluation.depth,
                done ? "Analysis replay finished." : null);
    }

    public synchronized AnalysisReplayStepDto state() {
        if (session == null) return null;
        return toStepDto(session, !session.active, null, null, null,
                latestEvaluation(session), latestBar(session), latestDepth(session), null);
    }

    public synchronized AnalysisReplayStepDto cancel() {
        if (session == null) return inactiveStep("No active analysis replay.");
        session.active = false;
        closeSessionEngine();
        return toStepDto(session, true, null, null, null,
                latestEvaluation(session), latestBar(session), latestDepth(session),
                "Analysis replay cancelled.");
    }

    public synchronized void clear() {
        closeSessionEngine();
        session = null;
    }

    private DeepAnalysisEngine createDeepAnalysisEngine(String enginePath) {
        if (enginePath == null || enginePath.isBlank()) {
            throw NativeEngineUnavailableException.startFailure(
                    NativeEngineRole.DEEP_ANALYSIS,
                    new IllegalStateException("Deep analysis engine profile has no executable path"));
        }
        try {
            DeepAnalysisUciEngine engine = new DeepAnalysisUciEngine(enginePath.trim());
            engine.setManagementLabel("deep analysis");
            return engine;
        } catch (Exception ex) {
            throw NativeEngineUnavailableException.startFailure(NativeEngineRole.DEEP_ANALYSIS, ex);
        }
    }

    private AnalysisEvaluation analyzeCurrentReplayPosition(AnalysisReplaySession source)
            throws NoMoveFoundException {
        AnalysisEvaluation terminalEvaluation = evaluateTerminalPosition(source);
        if (terminalEvaluation != null) return terminalEvaluation;
        try {
            source.engine.clearChachedLines();
            DeepAnalysisResult deepAnalysisResult = source.engine.analyze(source.replayGame, source.engineConfig);
            List<EngineLine> bestLines = deepAnalysisResult.getFinalLines();
            if (bestLines.isEmpty()) return new AnalysisEvaluation(0.0, 0.5, 0, List.of(), deepAnalysisResult);

            double eval = bestLines.get(0).getEvaluation();
            double bar = EvaluationBarMapper.toBar(eval);
            int depth = bestLines.get(0).getDepth();
            List<EngineLineDto> lines = new ArrayList<>();
            for (EngineLine line : bestLines) {
                Game displayGame = Simulation.forkDummyFrom(source.replayGame.getMoveList());
                lines.add(engineLineDisplayService.toDto(displayGame, line));
            }
            return new AnalysisEvaluation(eval, bar, depth, lines, deepAnalysisResult);
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
        if (source == null || source.replayGame == null) return null;
        Double evaluation = TerminalEvaluationMapper.toEvaluation(
                TerminalPositionEvaluator.determineState(source.replayGame));
        if (evaluation == null) return null;
        return new AnalysisEvaluation(
                evaluation, EvaluationBarMapper.toBar(evaluation), latestDepth(source), List.of());
    }

    private double latestEvaluation(AnalysisReplaySession source) {
        return source == null || source.profile.isEmpty() ? 0.0
                : source.profile.get(source.profile.size() - 1).getEvaluation();
    }

    private double latestBar(AnalysisReplaySession source) {
        return source == null || source.profile.isEmpty() ? 0.5
                : source.profile.get(source.profile.size() - 1).getBar();
    }

    private int latestDepth(AnalysisReplaySession source) {
        return source == null || source.profile.isEmpty() ? 0
                : source.profile.get(source.profile.size() - 1).getDepth();
    }

    private AnalysisReplayStepDto inactiveStep(String message) {
        return new AnalysisReplayStepDto(
                false, true, 0, 0, null, null, null,
                0.0, 0.5, 0, null, null, List.of(), message);
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
                source.active, done, source.originalMoves.size(), source.currentPly,
                from, to, san, Math.round(evaluation * 100.0) / 100.0,
                bar, depth, source.engineName, board,
                new ArrayList<>(source.profile), message);
    }

    private void closeSessionEngine() {
        if (session != null && session.engine != null) safeStop(session.engine);
    }

    private void safeStop(DeepAnalysisEngine engine) {
        if (engine == null) return;
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
        private final DeepAnalysisResult deepAnalysisResult;

        private AnalysisEvaluation(double evaluation, double bar, int depth, List<EngineLineDto> lines) {
            this(evaluation, bar, depth, lines, null);
        }

        private AnalysisEvaluation(
                double evaluation,
                double bar,
                int depth,
                List<EngineLineDto> lines,
                DeepAnalysisResult deepAnalysisResult) {
            this.evaluation = evaluation;
            this.bar = bar;
            this.depth = depth;
            this.lines = lines;
            this.deepAnalysisResult = deepAnalysisResult;
        }
    }

    private static class AnalysisReplaySession {
        private final List<Move> originalMoves;
        private final Game replayGame;
        private final DeepAnalysisEngine engine;
        private final UciEngineConfig engineConfig;
        private final String engineName;
        private final List<AnalysisProfilePointDto> profile = new ArrayList<>();
        private DeepAnalysisResult lastDeepAnalysisResult;
        private int currentPly;
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
