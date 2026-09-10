package demo.chess.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import demo.chess.api.dto.EngineEvaluationDto;
import demo.chess.api.dto.EngineLineDto;
import demo.chess.definitions.engines.EngineConfig;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.EvaluationEngine;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.impl.EvaluationUciEngine;
import demo.chess.game.Game;
import demo.chess.game.impl.Simulation;

@Service
public class EvaluationService {

    private static final Log logger = LogFactory.getLog(EvaluationService.class);

    private final GameService gameService;
    private final EngineRuntimeSelectionService engineRuntimeSelectionService;
    private final LiveEvaluationStreamService liveEvaluationStreamService;
    private final EngineLineDisplayService engineLineDisplayService;
    private final ExecutorService liveEvaluationPushExecutor;

    private EvaluationEngine evaluationEngine;
    private String currentEvaluationEnginePath;
    private long lastSeenSettingsVersion = -1L;

    /**
     * Creates a new EvaluationService instance.
     * @param gameService the game service
     * @param engineRuntimeSelectionService runtime engine profile selections
     * @param liveEvaluationStreamService SSE stream publisher
     * @param engineLineDisplayService canonical engine-line display converter
     */
    public EvaluationService(
            GameService gameService,
            EngineRuntimeSelectionService engineRuntimeSelectionService,
            LiveEvaluationStreamService liveEvaluationStreamService,
            EngineLineDisplayService engineLineDisplayService) {
        this.gameService = gameService;
        this.engineRuntimeSelectionService = engineRuntimeSelectionService;
        this.liveEvaluationStreamService = liveEvaluationStreamService;
        this.engineLineDisplayService = engineLineDisplayService;
        this.currentEvaluationEnginePath = engineRuntimeSelectionService.getEvaluationEnginePath();
        this.evaluationEngine = null;
        this.liveEvaluationPushExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "live-evaluation-sse-publisher");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Returns the evaluation.
     * @return the evaluation
     */
    public synchronized EngineEvaluationDto getEvaluation() {
        Game game = gameService.getCurrentGame();
        EngineConfig engineConfig = engineRuntimeSelectionService.getEvaluationConfig();
        EvaluationEngine engine = getEvaluationEngine();
        long settingsVersion = engineRuntimeSelectionService.getEvaluationVersion();

        logger.debug("Requesting best lines from engine (single snapshot)...");

        if (settingsVersion != lastSeenSettingsVersion) {
            engine.clearChachedLines();
            lastSeenSettingsVersion = settingsVersion;
        }

        List<EngineLine> bestLines;
        try {
            bestLines = engine.getBestLines(game, engineConfig);
        } catch (Exception e) {
            logger.error("Engine error while getting best lines: " + e.getMessage());
            e.printStackTrace();
            return new EngineEvaluationDto(0.0, 0.5, List.of());
        }

        int size = bestLines == null ? -1 : bestLines.size();
        logger.debug("Engine returned lines size=" + size);

        if (bestLines == null || bestLines.isEmpty()) {
            return new EngineEvaluationDto(0.0, 0.5, List.of());
        }

        return toEvaluationDto(
                game,
                bestLines,
                engineRuntimeSelectionService.getEvaluationEngineName());
    }

    /**
     * Evaluates the game for analysis.
     * @param game the game
     * @param engineConfig the engine config
     * @param moveTimeMillis the move time millis
     * @return the result of the operation
     */
    public synchronized EngineEvaluationDto evaluateGameForAnalysis(
            Game game,
            UciEngineConfig engineConfig,
            int moveTimeMillis) {
        EvaluationEngine engine = getEvaluationEngine();
        int safeMoveTimeMillis = Math.max(100, moveTimeMillis);

        try {
            engine.clearChachedLines();
            engine.getBestLines(game, engineConfig);
            Thread.sleep(safeMoveTimeMillis);
            List<EngineLine> bestLines = engine.getBestLines(game, engineConfig);
            engine.stopEvaluation();

            if (bestLines == null || bestLines.isEmpty()) {
                return new EngineEvaluationDto(0.0, 0.5, List.of());
            }

            double eval = bestLines.get(0).getEvaluation();
            double bar = EvaluationBarMapper.toBar(eval);
            List<EngineLineDto> lines = new ArrayList<>();

            for (EngineLine line : bestLines) {
                Game displayGame = Simulation.forkDummyFrom(game.getMoveList());
                lines.add(engineLineDisplayService.toDto(displayGame, line));
            }

            return new EngineEvaluationDto(eval, bar, lines);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            try {
                engine.stopEvaluation();
            } catch (Exception ignored) {
            }
            return new EngineEvaluationDto(0.0, 0.5, List.of());
        } catch (Exception e) {
            logger.error("Engine error while replay-analyzing position: " + e.getMessage());
            try {
                engine.stopEvaluation();
            } catch (Exception ignored) {
            }
            return new EngineEvaluationDto(0.0, 0.5, List.of());
        }
    }

    /**
     * Resets the for new game.
     */
    public synchronized void resetForNewGame() {
        logger.info("Resetting evaluation engine for new game");
        closeEvaluationEngine(evaluationEngine);
        evaluationEngine = null;
        currentEvaluationEnginePath = engineRuntimeSelectionService.getEvaluationEnginePath();
        lastSeenSettingsVersion = -1L;
    }

    /**
     * Stops the live evaluation.
     */
    public synchronized void stopLiveEvaluation() {
        logger.info("Stopping live evaluation engine");
        closeEvaluationEngine(evaluationEngine);
        evaluationEngine = null;
        lastSeenSettingsVersion = -1L;
    }

    private void handleEvaluationUpdate(String positionKey, List<EngineLine> lines) {
        if (lines == null || lines.isEmpty() || !liveEvaluationStreamService.hasSubscribers()) {
            return;
        }

        EngineLine principalLine = lines.get(0);
        int depth = principalLine.getDepth();
        if (!shouldPushDepth(depth)) {
            return;
        }

        double evaluation = principalLine.getEvaluation();
        liveEvaluationPushExecutor.execute(
                () -> publishBarSnapshot(positionKey, evaluation, depth));
    }

    private boolean shouldPushDepth(int depth) {
        return depth == 5 || depth == 10 || depth >= 15;
    }

    private void publishBarSnapshot(String positionKey, double evaluation, int depth) {
        try {
            Game currentGame = gameService.getCurrentGame();
            if (!positionKey.equals(currentGame.getMoveList().toString())) {
                return;
            }

            liveEvaluationStreamService.publish(
                    evaluation,
                    EvaluationBarMapper.toBar(evaluation),
                    depth);
        } catch (Exception e) {
            logger.debug("Could not publish live evaluation bar SSE snapshot: " + e.getMessage());
        }
    }

    /**
     * Converts one normal polled engine-line snapshot into the frontend DTO.
     * Chess notation is delegated to {@link EngineLineDisplayService}.
     */
    private EngineEvaluationDto toEvaluationDto(
            Game game,
            List<EngineLine> bestLines,
            String engineName) {
        double eval = bestLines.get(0).getEvaluation();
        double bar = EvaluationBarMapper.toBar(eval);

        List<EngineLineDto> lines = new ArrayList<>();
        for (EngineLine line : bestLines) {
            try {
                Game displayGame = Simulation.forkDummyFrom(game.getMoveList());
                lines.add(engineLineDisplayService.toDto(displayGame, line));
            } catch (Exception ex) {
                logger.error("Engine-line display conversion failed, fallback to UCI: " + ex.getMessage());
                double roundedEval = Math.round(line.getEvaluation() * 100.0) / 100.0;
                lines.add(new EngineLineDto(
                        roundedEval,
                        line.getDepth(),
                        line.getMateDistance(),
                        line.getMoves()));
            }
        }

        EngineEvaluationDto result = new EngineEvaluationDto(eval, bar, lines);
        result.setEngineName(engineName);
        return result;
    }

    private synchronized EvaluationEngine getEvaluationEngine() {
        String configuredPath = engineRuntimeSelectionService.getEvaluationEnginePath();
        if (evaluationEngine == null || !configuredPath.equals(currentEvaluationEnginePath)) {
            closeEvaluationEngine(evaluationEngine);
            currentEvaluationEnginePath = configuredPath;
            evaluationEngine = createEvaluationEngine(configuredPath);
            lastSeenSettingsVersion = -1L;
        }
        return evaluationEngine;
    }

    private EvaluationEngine createEvaluationEngine(String enginePath) {
        logger.info("Initializing evaluation engine at path: " + enginePath);
        try {
            EvaluationUciEngine engine = new EvaluationUciEngine(enginePath);
            engine.setManagementLabel("evaluation");
            engine.setEvaluationUpdateListener(this::handleEvaluationUpdate);
            return engine;
        } catch (Exception e) {
            logger.error("Could not start evaluation engine: " + e.getMessage());
            e.printStackTrace();
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
            logger.warn("Could not stop old evaluation engine: " + e.getMessage());
        }
        try {
            engine.close();
        } catch (Exception e) {
            logger.warn("Could not close old evaluation engine: " + e.getMessage());
        }
    }

}
