package demo.chess.api.service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import demo.chess.api.dto.MoveResultDto;
import demo.chess.definitions.Color;
import demo.chess.definitions.engines.EngineConfig;
import demo.chess.definitions.engines.PlayerEngine;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.engines.impl.PlayerUciEngine;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;

@Service
public class ComputerMoveService {

    private static final String COMPUTER_MOVE_CANCELLED_MESSAGE = "Computer move was cancelled";

    private static final Log logger = LogFactory.getLog(ComputerMoveService.class);

    private final GameService gameService;
    private final EngineSettingsService engineSettingsService;

    private PlayerEngine whitePlayerEngine;
    private PlayerEngine blackPlayerEngine;
    private String currentWhitePlayerEnginePath;
    private String currentBlackPlayerEnginePath;
    private long whitePlayerEngineGeneration;
    private long blackPlayerEngineGeneration;

    public ComputerMoveService(GameService gameService, EngineSettingsService engineSettingsService) {
        this.gameService = gameService;
        this.engineSettingsService = engineSettingsService;

        this.currentWhitePlayerEnginePath = engineSettingsService.getWhitePlayerEnginePath();
        this.currentBlackPlayerEnginePath = engineSettingsService.getBlackPlayerEnginePath();
        this.whitePlayerEngine = createPlayerEngineWithFallback(currentWhitePlayerEnginePath, "white player");
        this.blackPlayerEngine = createPlayerEngineWithFallback(currentBlackPlayerEnginePath, "black player");
    }

    public MoveResultDto makeComputerMove() throws NoMoveFoundException, IOException, InterruptedException, ExecutionException {
        Game game = gameService.getCurrentGame();

        if (game == null) {
            return new MoveResultDto(false, "No active game", null, null, null, null);
        }

        if (game.getState() != null) {
            String sideToMove = sideToMove(game);
            return new MoveResultDto(false, "Game is already finished: " + game.getState(), null, null, null, sideToMove,
                    gameService.getCurrentPositionString(), game.getState().name());
        }

        if (game.getPlayer() == null || game.getPlayer().getColor() == null) {
            return new MoveResultDto(false, "No side to move", null, null, null, null,
                    gameService.getCurrentPositionString(), null);
        }

        Color color = game.getPlayer().getColor();
        PlayerEngineSnapshot engineSnapshot = getPlayerEngineSnapshot(color);

        Move bestMove;
        try {
            bestMove = engineSnapshot.engine.getBestMove(game, engineSnapshot.config);
        } catch (NoMoveFoundException | IOException | InterruptedException | RuntimeException e) {
            if (isPlayerEngineGenerationChanged(color, engineSnapshot.generation)) {
                return computerMoveCancelledResult();
            }
            throw e;
        }

        if (isPlayerEngineGenerationChanged(color, engineSnapshot.generation)) {
            return computerMoveCancelledResult();
        }

        String from = bestMove.getSource() != null ? bestMove.getSource().getName() : null;
        String to = bestMove.getTarget() != null ? bestMove.getTarget().getName() : null;

        boolean applied = gameService.applyMoveIfCurrent(game, bestMove);
        if (!applied) {
            return new MoveResultDto(false, "Game has changed while the engine was thinking", from, to, null, null,
                    gameService.getCurrentPositionString(), null);
        }

        String san = lastSan(game);
        String sideToMove = sideToMove(game);
        String position = gameService.getCurrentPositionString();
        String gameState = game.getState() != null ? game.getState().name() : null;

        return new MoveResultDto(true, null, from, to, san, sideToMove, position, gameState);
    }

    /**
     * Stops and recreates the long-lived player engines for a clean new-game boundary.
     *
     * A new game must be able to cancel a currently thinking engine instead of waiting
     * for the old move to finish. Engine generations invalidate in-flight computer
     * moves before they can be applied to the current game.
     */
    public synchronized void resetForNewGame() {
        logger.info("Resetting player engines for new game");

        PlayerEngine oldWhitePlayerEngine = whitePlayerEngine;
        PlayerEngine oldBlackPlayerEngine = blackPlayerEngine;

        currentWhitePlayerEnginePath = engineSettingsService.getWhitePlayerEnginePath();
        currentBlackPlayerEnginePath = engineSettingsService.getBlackPlayerEnginePath();
        whitePlayerEngine = createPlayerEngineWithFallback(currentWhitePlayerEnginePath, "white player");
        blackPlayerEngine = createPlayerEngineWithFallback(currentBlackPlayerEnginePath, "black player");
        whitePlayerEngineGeneration++;
        blackPlayerEngineGeneration++;

        closePlayerEngine(oldWhitePlayerEngine, "previous white player");
        closePlayerEngine(oldBlackPlayerEngine, "previous black player");
    }

    /**
     * Cancels a currently running computer move for the selected side by replacing the
     * underlying UCI process and invalidating the in-flight engine generation.
     */
    public synchronized void cancelPlayerEngine(Color color) {
        if (color == Color.WHITE) {
            logger.info("Cancelling white player engine");
            PlayerEngine oldWhitePlayerEngine = whitePlayerEngine;
            currentWhitePlayerEnginePath = engineSettingsService.getWhitePlayerEnginePath();
            whitePlayerEngine = createPlayerEngineWithFallback(currentWhitePlayerEnginePath, "white player");
            whitePlayerEngineGeneration++;
            closePlayerEngine(oldWhitePlayerEngine, "cancelled white player");
            return;
        }

        logger.info("Cancelling black player engine");
        PlayerEngine oldBlackPlayerEngine = blackPlayerEngine;
        currentBlackPlayerEnginePath = engineSettingsService.getBlackPlayerEnginePath();
        blackPlayerEngine = createPlayerEngineWithFallback(currentBlackPlayerEnginePath, "black player");
        blackPlayerEngineGeneration++;
        closePlayerEngine(oldBlackPlayerEngine, "cancelled black player");
    }

    /**
     * Compatibility wrapper for older callers. The engine is no longer restricted to black;
     * it moves for whichever side is currently to move.
     */
    public MoveResultDto makeBlackComputerMove() throws NoMoveFoundException, IOException, InterruptedException, ExecutionException {
        return makeComputerMove();
    }

    private synchronized PlayerEngineSnapshot getPlayerEngineSnapshot(Color color) {
        if (color == Color.WHITE) {
            String configuredPath = engineSettingsService.getWhitePlayerEnginePath();
            if (!configuredPath.equals(currentWhitePlayerEnginePath)) {
                PlayerEngine oldWhitePlayerEngine = whitePlayerEngine;
                currentWhitePlayerEnginePath = configuredPath;
                whitePlayerEngine = createPlayerEngineWithFallback(configuredPath, "white player");
                whitePlayerEngineGeneration++;
                closePlayerEngine(oldWhitePlayerEngine, "white player");
            }
            return new PlayerEngineSnapshot(
                    whitePlayerEngine,
                    engineSettingsService.getWhitePlayerConfig(),
                    whitePlayerEngineGeneration);
        }

        String configuredPath = engineSettingsService.getBlackPlayerEnginePath();
        if (!configuredPath.equals(currentBlackPlayerEnginePath)) {
            PlayerEngine oldBlackPlayerEngine = blackPlayerEngine;
            currentBlackPlayerEnginePath = configuredPath;
            blackPlayerEngine = createPlayerEngineWithFallback(configuredPath, "black player");
            blackPlayerEngineGeneration++;
            closePlayerEngine(oldBlackPlayerEngine, "black player");
        }
        return new PlayerEngineSnapshot(
                blackPlayerEngine,
                engineSettingsService.getBlackPlayerConfig(),
                blackPlayerEngineGeneration);
    }

    private synchronized boolean isPlayerEngineGenerationChanged(Color color, long generation) {
        return color == Color.WHITE
                ? whitePlayerEngineGeneration != generation
                : blackPlayerEngineGeneration != generation;
    }

    private MoveResultDto computerMoveCancelledResult() {
        Game game = gameService.getCurrentGame();
        String sideToMove = game != null ? sideToMove(game) : null;
        String gameState = game != null && game.getState() != null ? game.getState().name() : null;
        return new MoveResultDto(false, COMPUTER_MOVE_CANCELLED_MESSAGE, null, null, null, sideToMove,
                gameService.getCurrentPositionString(), gameState);
    }

    private PlayerEngine createPlayerEngineWithFallback(String requestedPath, String label) {
        String defaultPath = engineSettingsService.getDefaultEnginePath();
        String effectiveRequestedPath = requestedPath == null || requestedPath.isBlank()
                ? defaultPath
                : requestedPath.trim();

        try {
            return createPlayerEngine(effectiveRequestedPath, label);
        } catch (RuntimeException ex) {
            if (defaultPath.equals(effectiveRequestedPath)) {
                throw ex;
            }

            logger.warn("Falling back to Stockfish for " + label + " after failing to start "
                    + effectiveRequestedPath + ": " + ex.getMessage());
            return createPlayerEngine(defaultPath, label + " fallback");
        }
    }

    private PlayerEngine createPlayerEngine(String enginePath, String label) {
        logger.info("Initializing " + label + " engine at path: " + enginePath);
        try {
            return new PlayerUciEngine(enginePath);
        } catch (Exception e) {
            logger.error("Could not start " + label + " engine: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Could not start " + label + " engine at " + enginePath, e);
        }
    }

    private void closePlayerEngine(PlayerEngine engine, String label) {
        if (engine == null) {
            return;
        }
        try {
            engine.close();
        } catch (Exception e) {
            logger.warn("Could not close " + label + " engine: " + e.getMessage());
        }
    }

    private String lastSan(Game game) {
        List<String> sanMoves = game.getSanMoveList();
        if (sanMoves == null || sanMoves.isEmpty()) {
            return null;
        }
        return sanMoves.get(sanMoves.size() - 1);
    }

    private String sideToMove(Game game) {
        return game.getPlayer() != null && game.getPlayer().getColor() != null
                ? game.getPlayer().getColor().name().toLowerCase(Locale.ROOT)
                : null;
    }

    private static final class PlayerEngineSnapshot {
        private final PlayerEngine engine;
        private final EngineConfig config;
        private final long generation;

        private PlayerEngineSnapshot(PlayerEngine engine, EngineConfig config, long generation) {
            this.engine = engine;
            this.config = config;
            this.generation = generation;
        }
    }
}
