package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import demo.chess.api.dto.EngineEvaluationDto;
import demo.chess.api.dto.EngineLineDto;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.EvaluationEngine;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.impl.EvaluationUciEngine;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.states.State;
import demo.chess.game.Game;
import demo.chess.game.impl.Simulation;

/**
 * Evaluates an arbitrary position from the currently loaded/played game while
 * the UI is in analysis mode.
 *
 * The configured default evaluation profile is deliberately used here instead
 * of the deep-analysis profile. The engine stays alive while the same ply is
 * polled so the infinite UCI search can refine its cached result. Selecting a
 * different ply resets that search cleanly.
 */
@Service
public class AnalysisEvaluationService {

    private static final Log logger = LogFactory.getLog(AnalysisEvaluationService.class);

    private final UciGameService uciGameService;
    private final EngineSettingsService engineSettingsService;

    private EvaluationEngine evaluationEngine;
    private String currentEvaluationEnginePath;
    private Integer currentPly;
    private long lastSeenSettingsVersion = -1L;

    public AnalysisEvaluationService(
            UciGameService uciGameService,
            EngineSettingsService engineSettingsService) {
        this.uciGameService = uciGameService;
        this.engineSettingsService = engineSettingsService;
    }

    public synchronized EngineEvaluationDto getEvaluation(int ply) {
        List<Move> originalMoves = uciGameService.getAnalysisMoveListSnapshot();
        if (ply < 1 || ply > originalMoves.size()) {
            throw new IllegalArgumentException(
                    "Analysis ply must be between 1 and " + originalMoves.size() + ", got " + ply);
        }

        try {
            Game game = createReplayGame(originalMoves, ply);
            EngineEvaluationDto terminalEvaluation = evaluateTerminalPosition(game);
            if (terminalEvaluation != null) {
                stopEvaluation();
                return terminalEvaluation;
            }

            UciEngineConfig engineConfig = engineSettingsService.toEvaluationEngineConfig();
            EvaluationEngine engine = getEvaluationEngine();
            long settingsVersion = engineSettingsService.getEvaluationVersion();

            if (currentPly == null
                    || currentPly.intValue() != ply
                    || settingsVersion != lastSeenSettingsVersion) {
                try {
                    engine.stopEvaluation();
                } catch (Exception ignored) {
                }
                engine.clearChachedLines();
                currentPly = ply;
                lastSeenSettingsVersion = settingsVersion;
            }

            List<EngineLine> bestLines = engine.getBestLines(game, engineConfig);
            if (bestLines == null || bestLines.isEmpty()) {
                EngineEvaluationDto result = new EngineEvaluationDto(0.0, 0.5, List.of());
                result.setEngineName(engineSettingsService.getEvaluationEngineName());
                return result;
            }

            double evaluation = bestLines.get(0).getEvaluation();
            double bar = mapEvalToBar(evaluation);
            List<EngineLineDto> lines = new ArrayList<>();

            for (EngineLine line : bestLines) {
                double roundedEvaluation = Math.round(line.getEvaluation() * 100.0) / 100.0;
                lines.add(new EngineLineDto(
                        roundedEvaluation,
                        line.getDepth(),
                        line.getMateDistance(),
                        line.getMoves()));
            }

            EngineEvaluationDto result = new EngineEvaluationDto(evaluation, bar, lines);
            result.setEngineName(engineSettingsService.getEvaluationEngineName());
            return result;
        } catch (NoMoveFoundException | IOException e) {
            throw new IllegalStateException("Could not reconstruct analysis position for ply " + ply, e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not evaluate analysis position for ply " + ply, e);
        }
    }

    public synchronized void stopEvaluation() {
        closeEvaluationEngine(evaluationEngine);
        evaluationEngine = null;
        currentEvaluationEnginePath = null;
        currentPly = null;
        lastSeenSettingsVersion = -1L;
    }

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

    private EngineEvaluationDto evaluateTerminalPosition(Game game) {
        if (game == null || game.getState() == null) {
            return null;
        }

        State state = game.getState();
        EngineEvaluationDto result;

        if (state == State.BLACK_MATED || state == State.BLACK_RESIGNED) {
            result = new EngineEvaluationDto(100.0, 1.0, List.of());
        } else if (state == State.WHITE_MATED || state == State.WHITE_RESIGNED) {
            result = new EngineEvaluationDto(-100.0, 0.0, List.of());
        } else if (state == State.STALEMATE
                || state == State.DRAW_BY_50_MOVES_RULE
                || state == State.DRAW_BY_THREEFOLD_REPETITION) {
            result = new EngineEvaluationDto(0.0, 0.5, List.of());
        } else {
            return null;
        }

        result.setEngineName(engineSettingsService.getEvaluationEngineName());
        return result;
    }

    private EvaluationEngine getEvaluationEngine() {
        String configuredPath = engineSettingsService.getEvaluationEnginePath();
        if (evaluationEngine == null || !configuredPath.equals(currentEvaluationEnginePath)) {
            closeEvaluationEngine(evaluationEngine);
            currentEvaluationEnginePath = configuredPath;
            evaluationEngine = createEvaluationEngine(configuredPath);
            currentPly = null;
            lastSeenSettingsVersion = -1L;
        }
        return evaluationEngine;
    }

    private EvaluationEngine createEvaluationEngine(String enginePath) {
        logger.info("Initializing analysis evaluation engine at path: " + enginePath);
        try {
            EvaluationUciEngine engine = new EvaluationUciEngine(enginePath);
            engine.setManagementLabel("analysis evaluation");
            return engine;
        } catch (Exception e) {
            throw new IllegalStateException("Could not start evaluation engine at " + enginePath, e);
        }
    }

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

    private double mapEvalToBar(double evaluation) {
        if (evaluation >= 99d) {
            return 1.0;
        }
        if (evaluation <= -99d) {
            return 0.0;
        }

        double result = 0.5 + Math.atan(Math.tan(Math.PI / 10d) * evaluation) / Math.PI;
        return Math.max(0.0, Math.min(1.0, result));
    }
}
