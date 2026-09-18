package demo.chess.api.service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import demo.chess.analysis.annotation.MoveAnnotation;
import demo.chess.analysis.annotation.MoveAnnotationClassifier;
import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.impl.EvaluationUciEngine;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.notation.UciMoveCodec;

/**
 * Independently re-classifies one historical move with the currently selected
 * runtime evaluation engine.
 *
 * <p>The normal analysis evaluation keeps searching the position after the
 * selected move so the existing continuation UI remains unchanged. This
 * service runs a second engine process on the position before the move and
 * accumulates one immutable MultiPV snapshot per completed depth. The existing
 * chess-core MoveAnnotationClassifier then receives a transient
 * DeepAnalysisResult built from those live snapshots.</p>
 */
@Service
public class AnalysisMoveAssessmentService {

    private static final Log logger =
            LogFactory.getLog(AnalysisMoveAssessmentService.class);

    private final AnalysisGameReplayService analysisGameReplayService;
    private final EngineRuntimeSelectionService engineRuntimeSelectionService;
    private final AnalysisEvaluationEngineFactory engineFactory;
    private final MoveAnnotationClassifier classifier =
            new MoveAnnotationClassifier();

    private final TreeMap<Integer, List<EngineLine>> depthHistory =
            new TreeMap<>();

    private EvaluationUciEngine assessmentEngine;
    private String currentAssessmentEnginePath;
    private String currentSelectionKey;
    private String currentEnginePositionKey;
    private long lastSeenSettingsVersion = -1L;

    @Autowired
    public AnalysisMoveAssessmentService(
            AnalysisGameReplayService analysisGameReplayService,
            EngineRuntimeSelectionService engineRuntimeSelectionService,
            AnalysisEvaluationEngineFactory engineFactory) {
        this.analysisGameReplayService = analysisGameReplayService;
        this.engineRuntimeSelectionService = engineRuntimeSelectionService;
        this.engineFactory = engineFactory;
    }

    /** Compatibility constructor retained for direct unit tests and embedders. */
    public AnalysisMoveAssessmentService(
            UciGameService uciGameService,
            EngineRuntimeSelectionService engineRuntimeSelectionService,
            AnalysisEvaluationEngineFactory engineFactory) {
        this(new AnalysisGameReplayService(uciGameService),
                engineRuntimeSelectionService, engineFactory);
    }

    /**
     * Returns the current live classification for one original-game move.
     *
     * @param ply selected original-game ply, starting at 1
     * @param resultingEvaluation current live evaluation of the position after
     *        the played move
     * @return live assessment state
     */
    public synchronized Result assess(int ply, double resultingEvaluation) {
        AnalysisGameContext context = analysisGameReplayService.currentContext();
        List<Move> originalMoves = context.moves();
        if (ply < 1 || ply > originalMoves.size()) {
            throw new IllegalArgumentException(
                    "Analysis ply must be between 1 and "
                    + originalMoves.size() + ", got " + ply);
        }

        try {
            Game positionBeforeMove = analysisGameReplayService.createPositionAtPly(
                    context, ply - 1);
            Move originalMove = originalMoves.get(ply - 1);
            Move replayMove = analysisGameReplayService.mapOriginalMove(
                    positionBeforeMove, originalMove);
            String playedMoveUci = UciMoveCodec.encode(positionBeforeMove, replayMove);

            return assessPosition(
                    positionBeforeMove,
                    playedMoveUci,
                    "ply:" + ply,
                    resultingEvaluation);
        } catch (NoMoveFoundException | IOException e) {
            throw new IllegalStateException(
                    "Could not reconstruct live move assessment for ply " + ply,
                    e);
        }
    }

    /**
     * Returns the current live classification for an arbitrary analysis move.
     *
     * <p>This is used for temporary analysis variations. The caller provides
     * the position before the move and a stable selection key. Nothing is
     * persisted by this service.</p>
     *
     * @param positionBeforeMove position before the candidate move
     * @param playedMoveUci candidate move in UCI form
     * @param selectionKey logical live-search identity
     * @param resultingEvaluation current evaluation after the candidate move
     * @return live assessment state
     */
    public synchronized Result assessPosition(
            Game positionBeforeMove,
            String playedMoveUci,
            String selectionKey,
            double resultingEvaluation) {
        try {
            Move replayMove = LegalMoveResolver.resolveUci(
                    positionBeforeMove,
                    playedMoveUci);

            UciEngineConfig config = createInfiniteEvaluationConfig();
            EvaluationUciEngine engine = getAssessmentEngine(config.getEngine());
            long settingsVersion =
                    engineRuntimeSelectionService.getEvaluationVersion();
            String enginePositionKey =
                    positionBeforeMove.getMoveList().toString();

            if (!Objects.equals(selectionKey, currentSelectionKey)
                    || !Objects.equals(
                            enginePositionKey,
                            currentEnginePositionKey)
                    || settingsVersion != lastSeenSettingsVersion) {
                resetSearchState(
                        engine,
                        selectionKey,
                        enginePositionKey,
                        settingsVersion);
            }

            List<EngineLine> finalLines =
                    engine.getBestLines(positionBeforeMove, config);
            if (finalLines == null || finalLines.isEmpty()) {
                return new Result(false, 0, null);
            }

            int depth = finalLines.get(0).getDepth();
            Map<Integer, List<EngineLine>> history =
                    copyDepthHistory();
            if (!history.containsKey(depth)) {
                history.put(depth, List.copyOf(finalLines));
            }

            DeepAnalysisResult liveResult =
                    new DeepAnalysisResult(finalLines, history);
            String canonicalPlayedMoveUci = UciMoveCodec.encode(
                    positionBeforeMove, replayMove);
            MoveAnnotation annotation = classifier.classify(
                    positionBeforeMove,
                    canonicalPlayedMoveUci,
                    liveResult,
                    resultingEvaluation);

            return new Result(true, depth, annotation);
        } catch (NoMoveFoundException | IOException e) {
            throw new IllegalStateException(
                    "Could not resolve or evaluate live assessment move "
                    + playedMoveUci,
                    e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Live move assessment interrupted for "
                    + selectionKey,
                    e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(
                    "Live move assessment failed for "
                    + selectionKey,
                    e);
        }
    }

    /**
     * Stops and releases the live move-assessment engine.
     */
    public synchronized void stopEvaluation() {
        closeAssessmentEngine();
        currentSelectionKey = null;
        currentEnginePositionKey = null;
        lastSeenSettingsVersion = -1L;
        depthHistory.clear();
    }

    private UciEngineConfig createInfiniteEvaluationConfig() {
        UciEngineConfig config =
                engineRuntimeSelectionService.requireEvaluationConfig();
        config.setDepth(0);
        config.setMoveTimeSeconds(0);
        return config;
    }

    private EvaluationUciEngine getAssessmentEngine(String configuredPath) {
        if (assessmentEngine == null
                || !configuredPath.equals(currentAssessmentEnginePath)) {
            EvaluationUciEngine replacement = engineFactory.create(
                    configuredPath,
                    "analysis move assessment");
            replacement.setEvaluationUpdateListener(this::recordDepthSnapshot);
            EvaluationUciEngine previous = assessmentEngine;

            assessmentEngine = replacement;
            currentAssessmentEnginePath = configuredPath;
            currentSelectionKey = null;
            currentEnginePositionKey = null;
            lastSeenSettingsVersion = -1L;
            depthHistory.clear();

            closeAssessmentEngine(previous);
        }
        return assessmentEngine;
    }

    private void resetSearchState(
            EvaluationUciEngine engine,
            String selectionKey,
            String enginePositionKey,
            long settingsVersion) {
        try {
            engine.stopEvaluation();
        } catch (Exception ignored) {
        }
        engine.clearChachedLines();
        currentSelectionKey = selectionKey;
        currentEnginePositionKey = enginePositionKey;
        lastSeenSettingsVersion = settingsVersion;
        depthHistory.clear();
    }

    private synchronized void recordDepthSnapshot(
            String positionKey,
            List<EngineLine> lines) {
        if (!Objects.equals(positionKey, currentEnginePositionKey)
                || lines == null
                || lines.isEmpty()) {
            return;
        }
        int depth = lines.get(0).getDepth();
        if (depth <= 0) {
            return;
        }
        depthHistory.put(depth, List.copyOf(lines));
    }

    private Map<Integer, List<EngineLine>> copyDepthHistory() {
        Map<Integer, List<EngineLine>> copy = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<EngineLine>> entry
                : depthHistory.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return copy;
    }

    private void closeAssessmentEngine() {
        EvaluationUciEngine previous = assessmentEngine;
        assessmentEngine = null;
        currentAssessmentEnginePath = null;
        closeAssessmentEngine(previous);
    }

    private void closeAssessmentEngine(EvaluationUciEngine engine) {
        if (engine == null) {
            return;
        }
        try {
            engine.stopEvaluation();
        } catch (Exception e) {
            logger.debug(
                    "Could not stop analysis move-assessment engine: "
                    + e.getMessage());
        }
        try {
            engine.close();
        } catch (Exception e) {
            logger.debug(
                    "Could not close analysis move-assessment engine: "
                    + e.getMessage());
        }
    }

    public record Result(
            boolean ready,
            int depth,
            MoveAnnotation annotation) {
    }
}
