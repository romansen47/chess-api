package demo.chess.api.service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import demo.chess.api.dto.MoveResultDto;
import demo.chess.api.engine.NativeEngineRole;
import demo.chess.api.exception.NativeEngineUnavailableException;
import demo.chess.definitions.Color;
import demo.chess.definitions.engines.EngineConfig;
import demo.chess.definitions.engines.PlayerEngine;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.engines.impl.PlayerUciEngine;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.notation.UciMoveCodec;

@Service
public class ComputerMoveService {

    private static final String COMPUTER_MOVE_CANCELLED_MESSAGE = "Computer move was cancelled";

    private static final Log logger = LogFactory.getLog(ComputerMoveService.class);

    private final GameService gameService;
    private final EngineRuntimeSelectionService engineRuntimeSelectionService;

    private PlayerEngine whitePlayerEngine;
    private PlayerEngine blackPlayerEngine;
    private String currentWhitePlayerEnginePath;
    private String currentBlackPlayerEnginePath;
    private long whitePlayerEngineGeneration;
    private long blackPlayerEngineGeneration;

    /**
     * Creates a new ComputerMoveService instance.
     * @param gameService the game service
     * @param engineRuntimeSelectionService runtime engine profile selections
     */
    public ComputerMoveService(
            GameService gameService,
            EngineRuntimeSelectionService engineRuntimeSelectionService) {
        this.gameService = gameService;
        this.engineRuntimeSelectionService = engineRuntimeSelectionService;

        this.currentWhitePlayerEnginePath = null;
        this.currentBlackPlayerEnginePath = null;
        this.whitePlayerEngine = null;
        this.blackPlayerEngine = null;
    }

    /**
     * Performs the make computer move operation.
     * @return the result of the operation
     */
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
        String uci = UciMoveCodec.encode(game, bestMove);

        boolean applied = gameService.applyMoveIfCurrent(game, bestMove);
        if (!applied) {
            return new MoveResultDto(false, "Game has changed while the engine was thinking", from, to, null, null,
                    gameService.getCurrentPositionString(), null);
        }

        String san = lastSan(game);
        String sideToMove = sideToMove(game);
        String position = gameService.getCurrentPositionString();
        String gameState = game.getState() != null ? game.getState().name() : null;

        MoveResultDto result = new MoveResultDto(
                true,
                null,
                from,
                to,
                san,
                sideToMove,
                position,
                gameState,
                game.getMoveList().size());
        result.setUci(uci);
        return result;
    }

    /** Resets player-engine processes for a new game. */
    public synchronized void resetForNewGame() {
        logger.info("Resetting player engines for new game");

        PlayerEngine oldWhitePlayerEngine = whitePlayerEngine;
        PlayerEngine oldBlackPlayerEngine = blackPlayerEngine;

        currentWhitePlayerEnginePath = null;
        currentBlackPlayerEnginePath = null;
        whitePlayerEngine = null;
        blackPlayerEngine = null;
        whitePlayerEngineGeneration++;
        blackPlayerEngineGeneration++;

        closePlayerEngine(oldWhitePlayerEngine, "previous white player");
        closePlayerEngine(oldBlackPlayerEngine, "previous black player");
    }

    /** Cancels the player engine for one color. */
    public synchronized void cancelPlayerEngine(Color color) {
        if (color == Color.WHITE) {
            logger.info("Cancelling white player engine");
            PlayerEngine oldWhitePlayerEngine = whitePlayerEngine;
            currentWhitePlayerEnginePath = null;
            whitePlayerEngine = null;
            whitePlayerEngineGeneration++;
            closePlayerEngine(oldWhitePlayerEngine, "cancelled white player");
            return;
        }

        logger.info("Cancelling black player engine");
        PlayerEngine oldBlackPlayerEngine = blackPlayerEngine;
        currentBlackPlayerEnginePath = null;
        blackPlayerEngine = null;
        blackPlayerEngineGeneration++;
        closePlayerEngine(oldBlackPlayerEngine, "cancelled black player");
    }

    /** Returns the currently configured player engine plus generation metadata. */
    private synchronized PlayerEngineSnapshot getPlayerEngineSnapshot(Color color) {
        if (color == Color.WHITE) {
            EngineConfig config = engineRuntimeSelectionService.getWhitePlayerConfig();
            String configuredPath = config.getEngine();
            if (whitePlayerEngine == null || !configuredPath.equals(currentWhitePlayerEnginePath)) {
                PlayerEngine replacement = createPlayerEngine(
                        configuredPath,
                        "white player",
                        NativeEngineRole.WHITE_PLAYER);
                PlayerEngine oldWhitePlayerEngine = whitePlayerEngine;

                whitePlayerEngine = replacement;
                currentWhitePlayerEnginePath = configuredPath;
                whitePlayerEngineGeneration++;

                closePlayerEngine(oldWhitePlayerEngine, "white player");
            }
            return new PlayerEngineSnapshot(
                    whitePlayerEngine,
                    config,
                    whitePlayerEngineGeneration);
        }

        EngineConfig config = engineRuntimeSelectionService.getBlackPlayerConfig();
        String configuredPath = config.getEngine();
        if (blackPlayerEngine == null || !configuredPath.equals(currentBlackPlayerEnginePath)) {
            PlayerEngine replacement = createPlayerEngine(
                    configuredPath,
                    "black player",
                    NativeEngineRole.BLACK_PLAYER);
            PlayerEngine oldBlackPlayerEngine = blackPlayerEngine;

            blackPlayerEngine = replacement;
            currentBlackPlayerEnginePath = configuredPath;
            blackPlayerEngineGeneration++;

            closePlayerEngine(oldBlackPlayerEngine, "black player");
        }
        return new PlayerEngineSnapshot(
                blackPlayerEngine,
                config,
                blackPlayerEngineGeneration);
    }

    /** Returns whether the player-engine assignment changed while it was thinking. */
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

    private PlayerEngine createPlayerEngine(
            String enginePath,
            String label,
            NativeEngineRole role) {
        logger.info("Initializing " + label + " engine at path: " + enginePath);
        try {
            PlayerUciEngine engine = new PlayerUciEngine(enginePath);
            engine.setManagementLabel(label);
            return engine;
        } catch (Exception e) {
            logger.error("Could not start " + label + " engine: " + e.getMessage());
            throw NativeEngineUnavailableException.startFailure(role, e);
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
