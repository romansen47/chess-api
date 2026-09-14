package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import demo.chess.api.dto.AnalysisVariationRequestDto;
import demo.chess.api.dto.EngineEvaluationDto;
import demo.chess.api.dto.EngineLineDto;
import demo.chess.api.mapper.MoveAnnotationDtoMapper;
import demo.chess.analysis.annotation.MoveAnnotation;
import demo.chess.analysis.annotation.MoveAnnotationClassifier;
import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.impl.EvaluationUciEngine;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.game.TerminalPositionEvaluator;
import demo.chess.game.impl.Simulation;

/**
 * Evaluates original-game and temporary variation positions while the UI is in
 * analysis mode.
 *
 * <p>The active runtime evaluation profile is deliberately used here instead
 * of the deep-analysis profile. One evaluation engine stays alive while the
 * selected position is polled. Its completed MultiPV depths are retained as a
 * snapshot when the user plays the next analysis move, so move annotations can
 * reuse the search that the user has just watched instead of starting a second
 * parallel engine process.</p>
 */
@Service
public class AnalysisEvaluationService {

    private static final Log logger =
            LogFactory.getLog(AnalysisEvaluationService.class);
    private static final int MAX_LIVE_SNAPSHOTS = 128;

    private final UciGameService uciGameService;
    private final AnalysisVariationService analysisVariationService;
    private final EngineRuntimeSelectionService engineRuntimeSelectionService;
    private final EngineLineDisplayService engineLineDisplayService;
    private final AnalysisEvaluationEngineFactory engineFactory;
    private final AnalysisReplayService analysisReplayService;
    private final MoveAnnotationClassifier moveAnnotationClassifier =
            new MoveAnnotationClassifier();

    private final TreeMap<Integer, List<EngineLine>> currentDepthHistory =
            new TreeMap<>();
    private final LinkedHashMap<String, LiveAnalysisSnapshot> liveSnapshots =
            new LinkedHashMap<>();

    private EvaluationUciEngine evaluationEngine;
    private String currentEvaluationEnginePath;
    private String currentPositionKey;
    private String currentEnginePositionKey;
    private long lastSeenSettingsVersion = -1L;
    private List<EngineLine> currentFinalLines = List.of();
    private EngineEvaluationDto lastValidEvaluation;
    private Integer historicalAssessmentTargetPly;
    private long historicalAssessmentSettingsVersion = -1L;
    private DeepAnalysisResult historicalAnalysisBeforeMove;

    /**
     * Creates a new AnalysisEvaluationService instance.
     *
     * @param uciGameService the uci game service
     * @param analysisVariationService the analysis variation service
     * @param engineRuntimeSelectionService runtime engine profile selections
     * @param engineLineDisplayService the engine line display service
     * @param engineFactory analysis evaluation engine factory
     * @param analysisReplayService completed deep-analysis results used only as
     *        a bootstrap when no live snapshot exists yet
     */
    public AnalysisEvaluationService(
            UciGameService uciGameService,
            AnalysisVariationService analysisVariationService,
            EngineRuntimeSelectionService engineRuntimeSelectionService,
            EngineLineDisplayService engineLineDisplayService,
            AnalysisEvaluationEngineFactory engineFactory,
            AnalysisReplayService analysisReplayService) {
        this.uciGameService = uciGameService;
        this.analysisVariationService = analysisVariationService;
        this.engineRuntimeSelectionService = engineRuntimeSelectionService;
        this.engineLineDisplayService = engineLineDisplayService;
        this.engineFactory = engineFactory;
        this.analysisReplayService = analysisReplayService;
    }

    /**
     * Returns the live evaluation for one original-game ply.
     *
     * <p>For a historical move, the single live evaluation engine first
     * analyzes the position before that move. As soon as a usable snapshot is
     * available, the same engine switches to the selected position and the
     * move is classified from the live before/after data. No second engine
     * instance is created.</p>
     *
     * @param ply the ply
     * @return the evaluation
     */
    public synchronized EngineEvaluationDto getEvaluation(int ply) {
        List<Move> originalMoves =
                uciGameService.getAnalysisMoveListSnapshot();
        if (ply < 1 || ply > originalMoves.size()) {
            throw new IllegalArgumentException(
                    "Analysis ply must be between 1 and "
                            + originalMoves.size() + ", got " + ply);
        }

        try {
            long settingsVersion =
                    engineRuntimeSelectionService.getEvaluationVersion();
            String configuredPath =
                    engineRuntimeSelectionService.getEvaluationEnginePath();

            resetForChangedSettingsIfNecessary(
                    settingsVersion,
                    configuredPath);
            ensureHistoricalAssessmentTarget(
                    ply,
                    settingsVersion);

            if (!hasUsableAnalysis(historicalAnalysisBeforeMove)) {
                String beforePositionKey =
                        "ply:" + (ply - 1);
                historicalAnalysisBeforeMove =
                        liveAnalysisForPosition(
                                beforePositionKey,
                                settingsVersion);

                if (!hasUsableAnalysis(historicalAnalysisBeforeMove)) {
                    Game positionBeforeMove =
                            createReplayGame(
                                    originalMoves,
                                    ply - 1);
                    evaluateGame(
                            positionBeforeMove,
                            beforePositionKey);
                    historicalAnalysisBeforeMove =
                            liveAnalysisForPosition(
                                    beforePositionKey,
                                    settingsVersion);
                }

                if (!hasUsableAnalysis(historicalAnalysisBeforeMove)) {
                    return pendingHistoricalEvaluation();
                }
            }

            Game game =
                    createReplayGame(originalMoves, ply);
            EngineEvaluationDto result =
                    evaluateGame(
                            game,
                            "ply:" + ply);

            boolean usableResult =
                    (result.getLines() != null
                            && !result.getLines().isEmpty())
                    || Math.abs(result.getEval()) >= 99;
            if (!usableResult) {
                return result;
            }

            Game positionBeforeMove =
                    createReplayGame(
                            originalMoves,
                            ply - 1);
            String playedMoveUci =
                    originalMoves.get(ply - 1).toString();
            MoveAnnotation annotation =
                    moveAnnotationClassifier.classify(
                            positionBeforeMove,
                            playedMoveUci,
                            historicalAnalysisBeforeMove,
                            result.getEval());

            result.setMoveAnnotationReady(true);
            result.setMoveAnnotationDepth(
                    finalDepth(
                            historicalAnalysisBeforeMove));
            result.setMoveAnnotation(
                    MoveAnnotationDtoMapper.toDto(annotation));
            return result;
        } catch (NoMoveFoundException | IOException e) {
            throw new IllegalStateException(
                    "Could not reconstruct analysis position for ply " + ply,
                    e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not evaluate analysis position for ply " + ply,
                    e);
        }
    }

    /**
     * Returns the evaluation for a temporary stateless variation.
     *
     * <p>The annotation of the newest variation move uses the retained live
     * search of its immediate predecessor position. For the first variation
     * move only, a completed DeepAnalysis result at the anchor ply is accepted
     * as a bootstrap when the live engine has not produced a usable snapshot
     * yet. No second evaluation engine is started for annotation generation.</p>
     *
     * @param request anchor ply plus variation moves
     * @return the evaluation
     */
    public synchronized EngineEvaluationDto getVariationEvaluation(
            AnalysisVariationRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Analysis variation request must not be null");
        }

        List<String> moves =
                request.getMoves() != null
                        ? request.getMoves()
                        : List.of();
        String positionKey =
                analysisPositionKey(request.getAnchorPly(), moves);

        try {
            Game game = analysisVariationService.createVariationGame(
                    request.getAnchorPly(),
                    moves);
            EngineEvaluationDto result =
                    evaluateGame(game, positionKey);

            if (moves.isEmpty()) {
                return result;
            }

            boolean usableResult =
                    (result.getLines() != null
                            && !result.getLines().isEmpty())
                    || Math.abs(result.getEval()) >= 99;
            if (!usableResult) {
                return result;
            }

            List<String> prefix =
                    moves.subList(0, moves.size() - 1);
            DeepAnalysisResult analysisBeforeMove =
                    analysisBeforeVariationMove(
                            request.getAnchorPly(),
                            prefix,
                            lastSeenSettingsVersion);
            if (!hasUsableAnalysis(analysisBeforeMove)) {
                return result;
            }

            String playedMoveUci =
                    moves.get(moves.size() - 1);
            Game positionBeforeMove =
                    analysisVariationService.createVariationGame(
                            request.getAnchorPly(),
                            prefix);

            MoveAnnotation annotation =
                    moveAnnotationClassifier.classify(
                            positionBeforeMove,
                            playedMoveUci,
                            analysisBeforeMove,
                            result.getEval());

            result.setMoveAnnotationReady(true);
            result.setMoveAnnotationDepth(
                    finalDepth(analysisBeforeMove));
            result.setMoveAnnotation(
                    MoveAnnotationDtoMapper.toDto(annotation));
            return result;
        } catch (NoMoveFoundException | IOException e) {
            throw new IllegalStateException(
                    "Could not reconstruct analysis variation",
                    e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not evaluate analysis variation",
                    e);
        }
    }

    private EngineEvaluationDto evaluateGame(
            Game game,
            String positionKey) throws Exception {
        long settingsVersion =
                engineRuntimeSelectionService.getEvaluationVersion();
        String configuredPath =
                engineRuntimeSelectionService.getEvaluationEnginePath();

        resetForChangedSettingsIfNecessary(
                settingsVersion,
                configuredPath);

        String enginePositionKey =
                game.getMoveList().toString();
        if (!positionKey.equals(currentPositionKey)) {
            snapshotCurrentPosition();
            stopCurrentSearch();
            if (evaluationEngine != null) {
                evaluationEngine.clearChachedLines();
            }

            currentPositionKey = positionKey;
            currentEnginePositionKey = enginePositionKey;
            currentDepthHistory.clear();
            currentFinalLines = List.of();
            lastValidEvaluation = null;
        }
        lastSeenSettingsVersion = settingsVersion;

        EngineEvaluationDto terminalEvaluation =
                evaluateTerminalPosition(game);
        if (terminalEvaluation != null) {
            stopCurrentSearch();
            return terminalEvaluation;
        }

        UciEngineConfig engineConfig =
                createInfiniteEvaluationConfig();
        EvaluationUciEngine engine = getEvaluationEngine();

        List<EngineLine> bestLines =
                engine.getBestLines(game, engineConfig);
        if (bestLines == null || bestLines.isEmpty()) {
            if (lastValidEvaluation != null) {
                return lastValidEvaluation;
            }

            EngineEvaluationDto result =
                    new EngineEvaluationDto(
                            0.0,
                            0.5,
                            List.of());
            result.setEngineName(
                    engineRuntimeSelectionService
                            .getEvaluationEngineName());
            return result;
        }

        rememberCurrentLines(
                enginePositionKey,
                bestLines);

        double evaluation =
                bestLines.get(0).getEvaluation();
        double bar =
                EvaluationBarMapper.toBar(evaluation);
        List<EngineLineDto> lines =
                new ArrayList<>();

        for (EngineLine line : bestLines) {
            Game displayGame =
                    Simulation.forkDummyFrom(
                            game.getMoveList());
            lines.add(
                    engineLineDisplayService.toDto(
                            displayGame,
                            line));
        }

        EngineEvaluationDto result =
                new EngineEvaluationDto(
                        evaluation,
                        bar,
                        lines);
        result.setEngineName(
                engineRuntimeSelectionService
                        .getEvaluationEngineName());
        lastValidEvaluation = result;
        return result;
    }

    private void resetForChangedSettingsIfNecessary(
            long settingsVersion,
            String configuredPath) {
        boolean settingsChanged =
                lastSeenSettingsVersion >= 0
                        && settingsVersion
                                != lastSeenSettingsVersion;
        boolean pathChanged =
                currentEvaluationEnginePath != null
                        && !currentEvaluationEnginePath
                                .equals(configuredPath);

        if (!settingsChanged && !pathChanged) {
            return;
        }

        closeEvaluationEngine(evaluationEngine);
        evaluationEngine = null;
        currentEvaluationEnginePath = null;
        currentPositionKey = null;
        currentEnginePositionKey = null;
        currentDepthHistory.clear();
        currentFinalLines = List.of();
        liveSnapshots.clear();
        lastValidEvaluation = null;
        historicalAssessmentTargetPly = null;
        historicalAssessmentSettingsVersion = -1L;
        historicalAnalysisBeforeMove = null;
        lastSeenSettingsVersion = -1L;
    }

    private void snapshotCurrentPosition() {
        if (currentPositionKey == null
                || lastSeenSettingsVersion < 0
                || currentFinalLines == null
                || currentFinalLines.isEmpty()) {
            return;
        }

        Map<Integer, List<EngineLine>> history =
                copyCurrentDepthHistory();
        int finalDepth =
                currentFinalLines.get(0).getDepth();
        if (finalDepth > 0
                && !history.containsKey(finalDepth)) {
            history.put(
                    finalDepth,
                    List.copyOf(currentFinalLines));
        }

        DeepAnalysisResult result =
                new DeepAnalysisResult(
                        currentFinalLines,
                        history);
        liveSnapshots.put(
                currentPositionKey,
                new LiveAnalysisSnapshot(
                        lastSeenSettingsVersion,
                        result));

        while (liveSnapshots.size()
                > MAX_LIVE_SNAPSHOTS) {
            String oldest =
                    liveSnapshots.keySet()
                            .iterator()
                            .next();
            liveSnapshots.remove(oldest);
        }
    }

    private void ensureHistoricalAssessmentTarget(
            int ply,
            long settingsVersion) {
        if (historicalAssessmentTargetPly != null
                && historicalAssessmentTargetPly == ply
                && historicalAssessmentSettingsVersion == settingsVersion) {
            return;
        }

        historicalAssessmentTargetPly = ply;
        historicalAssessmentSettingsVersion = settingsVersion;
        historicalAnalysisBeforeMove = null;
    }

    private DeepAnalysisResult liveAnalysisForPosition(
            String positionKey,
            long settingsVersion) {
        if (positionKey.equals(currentPositionKey)
                && settingsVersion == lastSeenSettingsVersion
                && currentFinalLines != null
                && !currentFinalLines.isEmpty()) {
            return new DeepAnalysisResult(
                    currentFinalLines,
                    copyCurrentDepthHistory());
        }

        LiveAnalysisSnapshot snapshot =
                liveSnapshots.get(positionKey);
        if (snapshot != null
                && snapshot.settingsVersion() == settingsVersion
                && hasUsableAnalysis(snapshot.result())) {
            return snapshot.result();
        }

        return null;
    }

    private EngineEvaluationDto pendingHistoricalEvaluation() {
        EngineEvaluationDto result =
                new EngineEvaluationDto(
                        0.0,
                        0.5,
                        List.of());
        result.setEngineName(
                engineRuntimeSelectionService
                        .getEvaluationEngineName());
        return result;
    }

    private DeepAnalysisResult analysisBeforeVariationMove(
            int anchorPly,
            List<String> prefix,
            long settingsVersion) {
        String prefixKey =
                analysisPositionKey(anchorPly, prefix);
        LiveAnalysisSnapshot snapshot =
                liveSnapshots.get(prefixKey);

        if (snapshot != null
                && snapshot.settingsVersion()
                        == settingsVersion
                && hasUsableAnalysis(snapshot.result())) {
            return snapshot.result();
        }

        if (prefix.isEmpty()) {
            DeepAnalysisResult fallback =
                    analysisReplayService
                            .getDeepAnalysisResultForPly(
                                    anchorPly);
            if (hasUsableAnalysis(fallback)) {
                return fallback;
            }
        }

        return null;
    }

    private boolean hasUsableAnalysis(
            DeepAnalysisResult result) {
        return result != null
                && result.getFinalLines() != null
                && !result.getFinalLines().isEmpty();
    }

    private int finalDepth(
            DeepAnalysisResult result) {
        if (!hasUsableAnalysis(result)) {
            return 0;
        }
        return result.getFinalLines()
                .get(0)
                .getDepth();
    }

    private static String analysisPositionKey(
            int anchorPly,
            List<String> moves) {
        if (moves == null || moves.isEmpty()) {
            return "ply:" + anchorPly;
        }
        return "variation:"
                + anchorPly
                + ":"
                + String.join(" ", moves);
    }

    private synchronized void recordDepthSnapshot(
            String enginePositionKey,
            List<EngineLine> lines) {
        rememberCurrentLines(
                enginePositionKey,
                lines);
    }

    private void rememberCurrentLines(
            String enginePositionKey,
            List<EngineLine> lines) {
        if (currentEnginePositionKey == null
                || !currentEnginePositionKey
                        .equals(enginePositionKey)
                || lines == null
                || lines.isEmpty()) {
            return;
        }

        List<EngineLine> snapshot =
                List.copyOf(lines);
        currentFinalLines = snapshot;

        int depth = snapshot.get(0).getDepth();
        if (depth > 0) {
            currentDepthHistory.put(
                    depth,
                    snapshot);
        }
    }

    private Map<Integer, List<EngineLine>>
            copyCurrentDepthHistory() {
        Map<Integer, List<EngineLine>> copy =
                new LinkedHashMap<>();
        for (Map.Entry<Integer, List<EngineLine>> entry
                : currentDepthHistory.entrySet()) {
            copy.put(
                    entry.getKey(),
                    List.copyOf(entry.getValue()));
        }
        return copy;
    }

    /**
     * Creates the infinite evaluation config.
     *
     * @return evaluation config
     */
    private UciEngineConfig createInfiniteEvaluationConfig() {
        UciEngineConfig config =
                engineRuntimeSelectionService
                        .getEvaluationConfig();
        config.setDepth(0);
        config.setMoveTimeSeconds(0);
        return config;
    }

    /**
     * Stops the evaluation and clears all transient live snapshots.
     */
    public synchronized void stopEvaluation() {
        closeEvaluationEngine(evaluationEngine);
        evaluationEngine = null;
        currentEvaluationEnginePath = null;
        currentPositionKey = null;
        currentEnginePositionKey = null;
        currentDepthHistory.clear();
        currentFinalLines = List.of();
        liveSnapshots.clear();
        lastSeenSettingsVersion = -1L;
        lastValidEvaluation = null;
    }

    private void stopCurrentSearch() {
        if (evaluationEngine == null) {
            return;
        }
        try {
            evaluationEngine.stopEvaluation();
        } catch (Exception e) {
            logger.debug(
                    "Could not stop current analysis evaluation: "
                            + e.getMessage());
        }
    }

    /**
     * Creates the replay game.
     *
     * @param originalMoves the original moves
     * @param ply the ply
     * @return reconstructed game
     */
    private Game createReplayGame(
            List<Move> originalMoves,
            int ply)
            throws NoMoveFoundException, IOException {
        Simulation replayGame =
                Simulation.createSimulation();

        for (int index = 0; index < ply; index++) {
            Move originalMove =
                    originalMoves.get(index);
            Move replayMove =
                    replayGame.getPlayer()
                            .getMoveInSimulation(
                                    replayGame,
                                    originalMove);
            replayGame.apply(replayMove);
        }

        return replayGame;
    }

    /**
     * Evaluates a terminal position.
     *
     * @param game the game
     * @return terminal evaluation or null
     */
    private EngineEvaluationDto evaluateTerminalPosition(
            Game game) {
        Double evaluation =
                TerminalEvaluationMapper.toEvaluation(
                        TerminalPositionEvaluator
                                .determineState(game));
        if (evaluation == null) {
            return null;
        }
        return terminalEvaluation(evaluation);
    }

    private EngineEvaluationDto terminalEvaluation(
            double evaluation) {
        EngineEvaluationDto result =
                new EngineEvaluationDto(
                        evaluation,
                        EvaluationBarMapper.toBar(
                                evaluation),
                        List.of());
        result.setEngineName(
                engineRuntimeSelectionService
                        .getEvaluationEngineName());
        return result;
    }

    /**
     * Returns the single live evaluation engine.
     *
     * @return evaluation engine
     */
    private EvaluationUciEngine getEvaluationEngine() {
        String configuredPath =
                engineRuntimeSelectionService
                        .getEvaluationEnginePath();
        if (evaluationEngine == null
                || !configuredPath.equals(
                        currentEvaluationEnginePath)) {
            closeEvaluationEngine(evaluationEngine);
            currentEvaluationEnginePath =
                    configuredPath;
            evaluationEngine =
                    engineFactory.create(
                            configuredPath,
                            "analysis evaluation");
            evaluationEngine
                    .setEvaluationUpdateListener(
                            this::recordDepthSnapshot);
        }
        return evaluationEngine;
    }

    private void closeEvaluationEngine(
            EvaluationUciEngine engine) {
        if (engine == null) {
            return;
        }
        try {
            engine.stopEvaluation();
        } catch (Exception e) {
            logger.debug(
                    "Could not stop analysis evaluation engine: "
                            + e.getMessage());
        }
        try {
            engine.close();
        } catch (Exception e) {
            logger.debug(
                    "Could not close analysis evaluation engine: "
                            + e.getMessage());
        }
    }

    private record LiveAnalysisSnapshot(
            long settingsVersion,
            DeepAnalysisResult result) {
    }
}
