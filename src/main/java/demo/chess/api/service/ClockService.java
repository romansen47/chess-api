package demo.chess.api.service;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.ClockDto;
import demo.chess.game.Game;

@Service
public class ClockService {

    private final GameService gameService;
    private final EngineSettingsService engineSettingsService;

    /**
     * Creates a new ClockService instance.
     * @param gameService the game service
     * @param engineSettingsService the engine settings service
     */
    public ClockService(GameService gameService, EngineSettingsService engineSettingsService) {
        this.gameService = gameService;
        this.engineSettingsService = engineSettingsService;
    }

    /**
     * Returns the clock.
     * @return the clock
     */
    public ClockDto getClock() {
        Game game = gameService.getCurrentGame();

        if (game == null) {
            return new ClockDto(
                    0,
                    0,
                    null,
                    false,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        int timeForEachPlayer = game.getTimeForEachPlayer();

        int whiteTime = timeForEachPlayer
                - (int) game.getWhitePlayer().getChessClock().getTime(TimeUnit.SECONDS);
        int blackTime = timeForEachPlayer
                - (int) game.getBlackPlayer().getChessClock().getTime(TimeUnit.SECONDS);

        String sideToMove = game.getPlayer() != null && game.getPlayer().getColor() != null
                ? game.getPlayer().getColor().name().toLowerCase(Locale.ROOT)
                : null;

        boolean whiteRunning = game.getWhitePlayer().getChessClock().isRunning();
        boolean blackRunning = game.getBlackPlayer().getChessClock().isRunning();

        String gameState = game.getState() != null ? game.getState().name() : null;
        String timeControl = formatTimeControl(game.getTimeForEachPlayer(), game.getIncrementForWhite(), game.getIncrementForBlack());
        String whitePlayerName = game.getWhitePlayer() != null ? game.getWhitePlayer().getName() : null;
        String blackPlayerName = game.getBlackPlayer() != null ? game.getBlackPlayer().getName() : null;
        String whitePlayerEngineName = engineSettingsService.getWhitePlayerEngineName();
        String blackPlayerEngineName = engineSettingsService.getBlackPlayerEngineName();

        return new ClockDto(
                Math.max(0, whiteTime),
                Math.max(0, blackTime),
                sideToMove,
                whiteRunning,
                blackRunning,
                gameState,
                timeControl,
                whitePlayerName,
                blackPlayerName,
                whitePlayerEngineName,
                blackPlayerEngineName);
    }

    /**
     * Formats the time control.
     * @param timeForEachPlayer the time for each player
     * @param incrementForWhite the increment for white
     * @param incrementForBlack the increment for black
     * @return the result of the operation
     */
    private String formatTimeControl(int timeForEachPlayer, int incrementForWhite, int incrementForBlack) {
        String base;

        if (timeForEachPlayer % 60 == 0) {
            base = String.valueOf(timeForEachPlayer / 60);
        } else {
            base = timeForEachPlayer + "s";
        }

        if (incrementForWhite == incrementForBlack) {
            return base + "+" + incrementForWhite;
        }

        return base + "+" + incrementForWhite + "/" + incrementForBlack;
    }
}