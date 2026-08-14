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
        this.whitePlayerEngine = null;
        this.blackPlayerEngine = null;
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
     * Stops any existing player engines for a clean new-game boundary.
     *
     * A new game must be able to cancel a currently thinking engine instead of waiting
     * for the old move to finish. Engine generations invalidate in-flight computer
     * moves before they can be applied to the current game. Engines stay dormant until
     * the next computer move actually needs them.
     */
    public synchronized void resetForNewGame() {
        logger.info("Resetting player engines for new game");

        PlayerEngine oldWhitePlayerEngine = whitePlayerEngine;
        PlayerEngine oldBlackPlayerEngine = blackPlayerEngine;

        currentWhitePlayerEnginePath = engineSettingsService.getWhitePlayerEnginePath();
        currentBlackPlayerEnginePath = engineSettingsService.getBlackPlayerEnginePath();
        whitePlayerEngine = null;
        blackPlayerEngine = null;
        whitePlayerEngineGeneration++;
        blackPlayerEngineGeneration++;

        closePlayerEngine(oldWhitePlayerEngine, "previous white player");
        closePlayerEngine(oldBlackPlayerEngine, "previous black player");
    }

    /**
     * Cancels a currently running computer move for the selected side, closes the
     * underlying UCI process and invalidates the in-flight engine generation. The
     * engine stays stopped until a later computer move actually needs it again.
     */
    public synchronized void cancelPlayerEngine(Color color) {
        if (color == Color.WHITE) {
            logger.info("Cancelling white player engine");
            PlayerEngine oldWhitePlayerEngine = whitePlayerEngine;
            currentWhitePlayerEnginePath = engineSettingsService.getWhitePlayerEnginePath();
            whitePlayerEngine = null;
            whitePlayerEngineGeneration++;
            closePlayerEngine(oldWhitePlayerEngine, "cancelled white player");
            return;
        }

        logger.info("Cancelling black player engine");
        PlayerEngine oldBlackPlayerEngine = blackPlayerEngine;
        currentBlackPlayerEnginePath = engineSettingsService.getBlackPlayerEnginePath();
        blackPlayerEngine = null;
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
            EngineConfig config = engineSettingsService.getWhitePlayerConfig();
            String configuredPath = config.getEngine();
            if (whitePlayerEngine == null || !configuredPath.equals(currentWhitePlayerEnginePath)) {
                PlayerEngine oldWhitePlayerEngine = whitePlayerEngine;
                currentWhitePlayerEnginePath = configuredPath;
                whitePlayerEngine = createPlayerEngine(configuredPath, "white player");
                whitePlayerEngineGeneration++;
                closePlayerEngine(oldWhitePlayerEngine, "white player");
            }
            return new PlayerEngineSnapshot(
                    whitePlayerEngine,
                    config,
                    whitePlayerEngineGeneration);
        }

        EngineConfig config = engineSettingsService.getBlackPlayerConfig();
        String configuredPath = config.getEngine();
        if (blackPlayerEngine == null || !configuredPath.equals(currentBlackPlayerEnginePath)) {
            PlayerEngine oldBlackPlayerEngine = blackPlayerEngine;
            currentBlackPlayerEnginePath = configuredPath;
            blackPlayerEngine = createPlayerEngine(configuredPath, "black player");
            blackPlayerEngineGeneration++;
            closePlayerEngine(oldBlackPlayerEngine, "black player");
        }
        return new PlayerEngineSnapshot(
                blackPlayerEngine,
                config,
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

    private PlayerEngine createPlayerEngine(String enginePath, String label) {
        logger.info("Initializing " + label + " engine at path: " + enginePath);
        try {
            PlayerUciEngine engine = new PlayerUciEngine(enginePath);
            engine.setManagementLabel(label);
            return engine;
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
