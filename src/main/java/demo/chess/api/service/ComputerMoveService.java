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

    private static final Log logger = LogFactory.getLog(ComputerMoveService.class);

    private final GameService gameService;
    private final EngineSettingsService engineSettingsService;

    private PlayerEngine whitePlayerEngine;
    private PlayerEngine blackPlayerEngine;
    private String currentWhitePlayerEnginePath;
    private String currentBlackPlayerEnginePath;

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
        PlayerEngine engine = getPlayerEngine(color);
        EngineConfig playerEngineConfig = color == Color.WHITE
                ? engineSettingsService.getWhitePlayerConfig()
                : engineSettingsService.getBlackPlayerConfig();

        Move bestMove = engine.getBestMove(game, playerEngineConfig);

        String from = bestMove.getSource() != null ? bestMove.getSource().getName() : null;
        String to = bestMove.getTarget() != null ? bestMove.getTarget().getName() : null;

        gameService.applyMove(bestMove);

        String san = lastSan(game);
        String sideToMove = sideToMove(game);
        String position = gameService.getCurrentPositionString();
        String gameState = game.getState() != null ? game.getState().name() : null;

        return new MoveResultDto(true, null, from, to, san, sideToMove, position, gameState);
    }

    /**
     * Compatibility wrapper for older callers. The engine is no longer restricted to black;
     * it moves for whichever side is currently to move.
     */
    public MoveResultDto makeBlackComputerMove() throws NoMoveFoundException, IOException, InterruptedException, ExecutionException {
        return makeComputerMove();
    }

    private synchronized PlayerEngine getPlayerEngine(Color color) {
        if (color == Color.WHITE) {
            String configuredPath = engineSettingsService.getWhitePlayerEnginePath();
            if (!configuredPath.equals(currentWhitePlayerEnginePath)) {
                closePlayerEngine(whitePlayerEngine, "white player");
                currentWhitePlayerEnginePath = configuredPath;
                whitePlayerEngine = createPlayerEngineWithFallback(configuredPath, "white player");
            }
            return whitePlayerEngine;
        }

        String configuredPath = engineSettingsService.getBlackPlayerEnginePath();
        if (!configuredPath.equals(currentBlackPlayerEnginePath)) {
            closePlayerEngine(blackPlayerEngine, "black player");
            currentBlackPlayerEnginePath = configuredPath;
            blackPlayerEngine = createPlayerEngineWithFallback(configuredPath, "black player");
        }
        return blackPlayerEngine;
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
            logger.warn("Could not close old " + label + " engine: " + e.getMessage());
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
}
