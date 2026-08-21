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
import demo.chess.definitions.players.Player;
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
    private final EngineLineDisplayService engineLineDisplayService;

    private EvaluationEngine evaluationEngine;
    private String currentEvaluationEnginePath;
    private Integer currentPly;
    private long lastSeenSettingsVersion = -1L;
    private EngineEvaluationDto lastValidEvaluation;

    public AnalysisEvaluationService(
            UciGameService uciGameService,
            EngineSettingsService engineSettingsService,
            EngineLineDisplayService engineLineDisplayService) {
        this.uciGameService = uciGameService;
        this.engineSettingsService = engineSettingsService;
        this.engineLineDisplayService = engineLineDisplayService;
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

            UciEngineConfig engineConfig = createInfiniteEvaluationConfig();
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
                lastValidEvaluation = null;
            }

            List<EngineLine> bestLines = engine.getBestLines(game, engineConfig);
            if (bestLines == null || bestLines.isEmpty()) {
                if (lastValidEvaluation != null) {
                    return lastValidEvaluation;
                }

                EngineEvaluationDto result = new EngineEvaluationDto(0.0, 0.5, List.of());
                result.setEngineName(engineSettingsService.getEvaluationEngineName());
                return result;
            }

            double evaluation = bestLines.get(0).getEvaluation();
            double bar = mapEvalToBar(evaluation);
            List<EngineLineDto> lines = new ArrayList<>();

            for (EngineLine line : bestLines) {
                Game displayGame = Simulation.forkDummyFrom(game.getMoveList());
                lines.add(engineLineDisplayService.toDto(displayGame, line));
            }

            EngineEvaluationDto result = new EngineEvaluationDto(evaluation, bar, lines);
            result.setEngineName(engineSettingsService.getEvaluationEngineName());
            lastValidEvaluation = result;
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
     * Resolve the profile assigned under Defaults -> Evaluation and explicitly
     * remove all finite-search limits. EvaluationUciEngine itself uses
     * "go infinite"; keeping depth and move time at zero also prevents parser
     * or future engine code from accidentally inheriting a DeepAnalysis limit.
     */
    private UciEngineConfig createInfiniteEvaluationConfig() {
        String profileId = engineSettingsService.getDefaultEvaluationProfileId();
        UciEngineConfig config = engineSettingsService.getConfig(profileId);
        config.setDepth(0);
        config.setMoveTimeSeconds(0);
        return config;
    }

    public synchronized void stopEvaluation() {
        closeEvaluationEngine(evaluationEngine);
        evaluationEngine = null;
        currentEvaluationEnginePath = null;
        currentPly = null;
        lastSeenSettingsVersion = -1L;
        lastValidEvaluation = null;
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
        if (game == null) {
            return null;
        }

        EngineEvaluationDto explicitStateEvaluation = evaluateExplicitTerminalState(game);
        if (explicitStateEvaluation != null) {
            return explicitStateEvaluation;
        }

        return evaluateTerminalSimulationPosition(game);
    }

    private EngineEvaluationDto evaluateExplicitTerminalState(Game game) {
        State state = game.getState();
        if (state == null) {
            return null;
        }

        if (state == State.BLACK_MATED || state == State.BLACK_RESIGNED) {
            return terminalEvaluation(100.0, 1.0);
        }

        if (state == State.WHITE_MATED || state == State.WHITE_RESIGNED) {
            return terminalEvaluation(-100.0, 0.0);
        }

        if (state == State.STALEMATE
                || state == State.DRAW_BY_50_MOVES_RULE
                || state == State.DRAW_BY_THREEFOLD_REPETITION) {
            return terminalEvaluation(0.0, 0.5);
        }

        return null;
    }

    private EngineEvaluationDto evaluateTerminalSimulationPosition(Game game) {
        Player playerToMove = game.getPlayer();
        if (playerToMove == null || playerToMove.getKing() == null || playerToMove.getKing().getField() == null) {
            return null;
        }

        try {
            if (!playerToMove.getValidMoves(game).isEmpty()) {
                return null;
            }
        } catch (NoMoveFoundException | IOException e) {
            return null;
        }

        Player opponent = playerToMove == game.getWhitePlayer()
                ? game.getBlackPlayer()
                : game.getWhitePlayer();

        boolean kingIsAttacked = opponent.getSimpleMoves().stream()
                .map(Move::getTarget)
                .anyMatch(playerToMove.getKing().getField()::equals);

        if (!kingIsAttacked) {
            return terminalEvaluation(0.0, 0.5);
        }

        return playerToMove == game.getWhitePlayer()
                ? terminalEvaluation(-100.0, 0.0)
                : terminalEvaluation(100.0, 1.0);
    }

    private EngineEvaluationDto terminalEvaluation(double evaluation, double bar) {
        EngineEvaluationDto result = new EngineEvaluationDto(evaluation, bar, List.of());
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
            lastValidEvaluation = null;
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
