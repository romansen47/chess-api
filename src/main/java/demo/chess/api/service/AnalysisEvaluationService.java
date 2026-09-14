package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import demo.chess.api.dto.AnalysisVariationRequestDto;
import demo.chess.api.dto.EngineEvaluationDto;
import demo.chess.api.dto.EngineLineDto;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.EvaluationEngine;
import demo.chess.definitions.engines.UciEngineConfig;
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
 * of the deep-analysis profile. The engine stays alive while the same logical
 * position is polled so the infinite UCI search can refine its cached result.
 * Selecting a different original ply, variation or runtime evaluation profile
 * resets that search cleanly.</p>
 */
@Service
public class AnalysisEvaluationService {

    private static final Log logger = LogFactory.getLog(AnalysisEvaluationService.class);

    private final UciGameService uciGameService;
    private final AnalysisVariationService analysisVariationService;
    private final EngineRuntimeSelectionService engineRuntimeSelectionService;
    private final EngineLineDisplayService engineLineDisplayService;
    private final AnalysisEvaluationEngineFactory engineFactory;
    private final AnalysisMoveAssessmentService moveAssessmentService;

    private EvaluationEngine evaluationEngine;
    private String currentEvaluationEnginePath;
    private String currentPositionKey;
    private long lastSeenSettingsVersion = -1L;
    private EngineEvaluationDto lastValidEvaluation;

    /**
     * Creates a new AnalysisEvaluationService instance.
     * @param uciGameService the uci game service
     * @param analysisVariationService the analysis variation service
     * @param engineRuntimeSelectionService runtime engine profile selections
     * @param engineLineDisplayService the engine line display service
     * @param engineFactory analysis evaluation engine factory
     * @param moveAssessmentService live historical move assessment
     */
    public AnalysisEvaluationService(
            UciGameService uciGameService,
            AnalysisVariationService analysisVariationService,
            EngineRuntimeSelectionService engineRuntimeSelectionService,
            EngineLineDisplayService engineLineDisplayService,
            AnalysisEvaluationEngineFactory engineFactory,
            AnalysisMoveAssessmentService moveAssessmentService) {
        this.uciGameService = uciGameService;
        this.analysisVariationService = analysisVariationService;
        this.engineRuntimeSelectionService = engineRuntimeSelectionService;
        this.engineLineDisplayService = engineLineDisplayService;
        this.engineFactory = engineFactory;
        this.moveAssessmentService = moveAssessmentService;
    }

    /**
     * Returns the evaluation for one original-game ply.
     * @param ply the ply
     * @return the evaluation
     */
    public synchronized EngineEvaluationDto getEvaluation(int ply) {
        List<Move> originalMoves = uciGameService.getAnalysisMoveListSnapshot();
        if (ply < 1 || ply > originalMoves.size()) {
            throw new IllegalArgumentException(
                    "Analysis ply must be between 1 and " + originalMoves.size() + ", got " + ply);
        }

        try {
            Game game = createReplayGame(originalMoves, ply);
            EngineEvaluationDto result = evaluateGame(game, "ply:" + ply);
            boolean usableResult =
                    (result.getLines() != null && !result.getLines().isEmpty())
                    || Math.abs(result.getEval()) >= 99;
            if (usableResult) {
                AnalysisMoveAssessmentService.Result assessment =
                        moveAssessmentService.assess(ply, result.getEval());
                result.setMoveAnnotationReady(assessment.ready());
                result.setMoveAnnotationDepth(assessment.depth());
                result.setMoveAnnotation(
                        demo.chess.api.mapper.MoveAnnotationDtoMapper.toDto(
                                assessment.annotation()));
            }
            return result;
        } catch (NoMoveFoundException | IOException e) {
            throw new IllegalStateException("Could not reconstruct analysis position for ply " + ply, e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not evaluate analysis position for ply " + ply, e);
        }
    }

    /**
     * Returns the evaluation for a stateless temporary variation.
     * @param request anchor ply plus variation moves
     * @return the evaluation
     */
    public synchronized EngineEvaluationDto getVariationEvaluation(AnalysisVariationRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Analysis variation request must not be null");
        }

        List<String> moves = request.getMoves() != null ? request.getMoves() : List.of();
        String positionKey = "variation:" + request.getAnchorPly() + ":" + String.join(" ", moves);

        try {
            Game game = analysisVariationService.createVariationGame(
                    request.getAnchorPly(),
                    moves);
            EngineEvaluationDto result = evaluateGame(game, positionKey);

            boolean usableResult =
                    (result.getLines() != null && !result.getLines().isEmpty())
                    || Math.abs(result.getEval()) >= 99;

            if (moves.isEmpty()) {
                moveAssessmentService.stopEvaluation();
                return result;
            }

            if (usableResult) {
                List<String> prefix = moves.subList(0, moves.size() - 1);
                String playedMoveUci = moves.get(moves.size() - 1);
                Game positionBeforeMove =
                        analysisVariationService.createVariationGame(
                                request.getAnchorPly(),
                                prefix);

                AnalysisMoveAssessmentService.Result assessment =
                        moveAssessmentService.assessPosition(
                                positionBeforeMove,
                                playedMoveUci,
                                positionKey,
                                result.getEval());

                result.setMoveAnnotationReady(assessment.ready());
                result.setMoveAnnotationDepth(assessment.depth());
                result.setMoveAnnotation(
                        demo.chess.api.mapper.MoveAnnotationDtoMapper.toDto(
                                assessment.annotation()));
            }

            return result;
        } catch (NoMoveFoundException | IOException e) {
            throw new IllegalStateException("Could not reconstruct analysis variation", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not evaluate analysis variation", e);
        }
    }

    private EngineEvaluationDto evaluateGame(Game game, String positionKey) throws Exception {
        EngineEvaluationDto terminalEvaluation = evaluateTerminalPosition(game);
        if (terminalEvaluation != null) {
            stopContinuationEvaluation();
            return terminalEvaluation;
        }

        UciEngineConfig engineConfig = createInfiniteEvaluationConfig();
        EvaluationEngine engine = getEvaluationEngine();
        long settingsVersion = engineRuntimeSelectionService.getEvaluationVersion();

        if (!positionKey.equals(currentPositionKey)
                || settingsVersion != lastSeenSettingsVersion) {
            try {
                engine.stopEvaluation();
            } catch (Exception ignored) {
            }
            engine.clearChachedLines();
            currentPositionKey = positionKey;
            lastSeenSettingsVersion = settingsVersion;
            lastValidEvaluation = null;
        }

        List<EngineLine> bestLines = engine.getBestLines(game, engineConfig);
        if (bestLines == null || bestLines.isEmpty()) {
            if (lastValidEvaluation != null) {
                return lastValidEvaluation;
            }

            EngineEvaluationDto result = new EngineEvaluationDto(0.0, 0.5, List.of());
            result.setEngineName(engineRuntimeSelectionService.getEvaluationEngineName());
            return result;
        }

        double evaluation = bestLines.get(0).getEvaluation();
        double bar = EvaluationBarMapper.toBar(evaluation);
        List<EngineLineDto> lines = new ArrayList<>();

        for (EngineLine line : bestLines) {
            Game displayGame = Simulation.forkDummyFrom(game.getMoveList());
            lines.add(engineLineDisplayService.toDto(displayGame, line));
        }

        EngineEvaluationDto result = new EngineEvaluationDto(evaluation, bar, lines);
        result.setEngineName(engineRuntimeSelectionService.getEvaluationEngineName());
        lastValidEvaluation = result;
        return result;
    }

    /**
     * Creates the infinite evaluation config.
     * @return the result of the operation
     */
    private UciEngineConfig createInfiniteEvaluationConfig() {
        UciEngineConfig config = engineRuntimeSelectionService.getEvaluationConfig();
        config.setDepth(0);
        config.setMoveTimeSeconds(0);
        return config;
    }

    /**
     * Stops the evaluation.
     */
    public synchronized void stopEvaluation() {
        stopContinuationEvaluation();
        moveAssessmentService.stopEvaluation();
    }

    private void stopContinuationEvaluation() {
        closeEvaluationEngine(evaluationEngine);
        evaluationEngine = null;
        currentEvaluationEnginePath = null;
        currentPositionKey = null;
        lastSeenSettingsVersion = -1L;
        lastValidEvaluation = null;
    }

    /**
     * Creates the replay game.
     * @param originalMoves the original moves
     * @param ply the ply
     * @return the result of the operation
     */
    private Game createReplayGame(List<Move> originalMoves, int ply)
            throws NoMoveFoundException, IOException {
        Simulation replayGame = Simulation.createSimulation();

        for (int index = 0; index < ply; index++) {
            Move originalMove = originalMoves.get(index);
            Move replayMove = replayGame.getPlayer().getMoveInSimulation(replayGame, originalMove);
            replayGame.apply(replayMove);
        }

        return replayGame;
    }

    /**
     * Evaluates the terminal position.
     * @param game the game
     * @return the result of the operation
     */
    private EngineEvaluationDto evaluateTerminalPosition(Game game) {
        Double evaluation = TerminalEvaluationMapper.toEvaluation(
                TerminalPositionEvaluator.determineState(game));
        if (evaluation == null) {
            return null;
        }
        return terminalEvaluation(evaluation);
    }


    /**
     * Performs the terminal evaluation operation.
     * @param evaluation the evaluation
     * @return the result of the operation
     */
    private EngineEvaluationDto terminalEvaluation(double evaluation) {
        EngineEvaluationDto result = new EngineEvaluationDto(
                evaluation,
                EvaluationBarMapper.toBar(evaluation),
                List.of());
        result.setEngineName(engineRuntimeSelectionService.getEvaluationEngineName());
        return result;
    }

    /**
     * Returns the evaluation engine.
     * @return the evaluation engine
     */
    private EvaluationEngine getEvaluationEngine() {
        String configuredPath = engineRuntimeSelectionService.getEvaluationEnginePath();
        if (evaluationEngine == null || !configuredPath.equals(currentEvaluationEnginePath)) {
            closeEvaluationEngine(evaluationEngine);
            currentEvaluationEnginePath = configuredPath;
            evaluationEngine = engineFactory.create(
                    configuredPath,
                    "analysis evaluation");
            currentPositionKey = null;
            lastSeenSettingsVersion = -1L;
            lastValidEvaluation = null;
        }
        return evaluationEngine;
    }

    /**
     * Closes the evaluation engine.
     * @param engine the engine
     */
    private void closeEvaluationEngine(EvaluationEngine engine) {
        if (engine == null) {
            return;
        }
        try {
            engine.stopEvaluation();
        } catch (Exception e) {
            logger.debug("Could not stop analysis evaluation engine: " + e.getMessage());
        }
        try {
            engine.close();
        } catch (Exception e) {
            logger.debug("Could not close analysis evaluation engine: " + e.getMessage());
        }
    }

}
