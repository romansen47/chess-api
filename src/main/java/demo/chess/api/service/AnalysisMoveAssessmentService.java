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
import demo.chess.game.impl.Simulation;

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

    private final UciGameService uciGameService;
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

    public AnalysisMoveAssessmentService(
            UciGameService uciGameService,
            EngineRuntimeSelectionService engineRuntimeSelectionService,
            AnalysisEvaluationEngineFactory engineFactory) {
        this.uciGameService = uciGameService;
        this.engineRuntimeSelectionService = engineRuntimeSelectionService;
        this.engineFactory = engineFactory;
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
        List<Move> originalMoves = uciGameService.getAnalysisMoveListSnapshot();
        if (ply < 1 || ply > originalMoves.size()) {
            throw new IllegalArgumentException(
                    "Analysis ply must be between 1 and "
                    + originalMoves.size() + ", got " + ply);
        }

        try {
            Game positionBeforeMove = createReplayGame(originalMoves, ply - 1);
            Move originalMove = originalMoves.get(ply - 1);
            Move replayMove = positionBeforeMove.getPlayer()
                    .getMoveInSimulation(positionBeforeMove, originalMove);
            if (replayMove == null) {
                throw new NoMoveFoundException(
                        "Could not map live assessment move: " + originalMove);
            }

            UciEngineConfig config = createInfiniteEvaluationConfig();
            EvaluationUciEngine engine = getAssessmentEngine();
            long settingsVersion =
                    engineRuntimeSelectionService.getEvaluationVersion();
            String enginePositionKey =
                    positionBeforeMove.getMoveList().toString();
            String selectionKey = "ply:" + ply;

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
            MoveAnnotation annotation = classifier.classify(
                    positionBeforeMove,
                    replayMove.toString(),
                    liveResult,
                    resultingEvaluation);

            return new Result(true, depth, annotation);
        } catch (NoMoveFoundException | IOException e) {
            throw new IllegalStateException(
                    "Could not reconstruct live move assessment for ply " + ply,
                    e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Live move assessment interrupted for ply " + ply,
                    e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(
                    "Live move assessment failed for ply " + ply,
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
                engineRuntimeSelectionService.getEvaluationConfig();
        config.setDepth(0);
        config.setMoveTimeSeconds(0);
        return config;
    }

    private EvaluationUciEngine getAssessmentEngine() {
        String configuredPath =
                engineRuntimeSelectionService.getEvaluationEnginePath();
        if (assessmentEngine == null
                || !configuredPath.equals(currentAssessmentEnginePath)) {
            closeAssessmentEngine();
            currentAssessmentEnginePath = configuredPath;
            assessmentEngine = engineFactory.create(
                    configuredPath,
                    "analysis move assessment");
            assessmentEngine.setEvaluationUpdateListener(
                    this::recordDepthSnapshot);
            currentSelectionKey = null;
            currentEnginePositionKey = null;
            lastSeenSettingsVersion = -1L;
            depthHistory.clear();
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

    private Game createReplayGame(List<Move> originalMoves, int ply)
            throws NoMoveFoundException, IOException {
        Simulation replayGame = Simulation.createSimulation();
        for (int index = 0; index < ply; index++) {
            Move originalMove = originalMoves.get(index);
            Move replayMove = replayGame.getPlayer()
                    .getMoveInSimulation(replayGame, originalMove);
            if (replayMove == null) {
                throw new NoMoveFoundException(
                        "Could not map live assessment replay move: "
                        + originalMove);
            }
            replayGame.apply(replayMove);
        }
        return replayGame;
    }

    private void closeAssessmentEngine() {
        if (assessmentEngine == null) {
            return;
        }
        try {
            assessmentEngine.stopEvaluation();
        } catch (Exception e) {
            logger.debug(
                    "Could not stop analysis move-assessment engine: "
                    + e.getMessage());
        }
        try {
            assessmentEngine.close();
        } catch (Exception e) {
            logger.debug(
                    "Could not close analysis move-assessment engine: "
                    + e.getMessage());
        }
        assessmentEngine = null;
        currentAssessmentEnginePath = null;
    }

    public record Result(
            boolean ready,
            int depth,
            MoveAnnotation annotation) {
    }
}
