package demo.chess.api.service;

import java.util.Locale;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.ClockDto;
import demo.chess.definitions.clocks.impl.ChessClock;
import demo.chess.game.Game;

/**
 * Projects the core-owned chess clocks into the API representation.
 *
 * <p>The service does not own timekeeping. It reads the current game clocks,
 * formats the configured time control and enriches the response with the
 * current runtime engine labels.</p>
 */
@Service
public class ClockService {

    private final GameService gameService;
    private final EngineRuntimeSelectionService engineRuntimeSelectionService;

    /**
     * Creates the clock projection service.
     *
     * @param gameService owner of the current game
     * @param engineRuntimeSelectionService runtime engine profile selections
     */
    public ClockService(
            GameService gameService,
            EngineRuntimeSelectionService engineRuntimeSelectionService) {
        this.gameService = gameService;
        this.engineRuntimeSelectionService = engineRuntimeSelectionService;
    }

    /**
     * Returns the current clock and player/engine display state.
     *
     * @return current clock DTO
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

        int whiteTime = remainingSeconds(game.getWhitePlayer().getChessClock());
        int blackTime = remainingSeconds(game.getBlackPlayer().getChessClock());

        String sideToMove = game.getPlayer() != null && game.getPlayer().getColor() != null
                ? game.getPlayer().getColor().name().toLowerCase(Locale.ROOT)
                : null;

        boolean whiteRunning = game.getWhitePlayer().getChessClock().isRunning();
        boolean blackRunning = game.getBlackPlayer().getChessClock().isRunning();

        String gameState = game.getState() != null ? game.getState().name() : null;
        String timeControl = formatTimeControl(game.getTimeForEachPlayer(), game.getIncrementForWhite(), game.getIncrementForBlack());
        String whitePlayerName = game.getWhitePlayer() != null ? game.getWhitePlayer().getName() : null;
        String blackPlayerName = game.getBlackPlayer() != null ? game.getBlackPlayer().getName() : null;
        String whitePlayerEngineName = engineRuntimeSelectionService.getWhitePlayerEngineName();
        String blackPlayerEngineName = engineRuntimeSelectionService.getBlackPlayerEngineName();

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
     * Converts the core clock's remaining milliseconds to the whole seconds shown
     * by the frontend. Rounding up preserves the existing countdown display.
     *
     * @param clock core-owned chess clock
     * @return remaining display seconds
     */
    private int remainingSeconds(ChessClock clock) {
        if (clock == null) {
            return 0;
        }

        long remainingMillis = clock.getRemainingTimeMillis();
        long seconds = (remainingMillis + 999L) / 1_000L;
        return (int) Math.min(Integer.MAX_VALUE, seconds);
    }

    /**
     * Formats the configured base time and asymmetric increments for display.
     *
     * @param timeForEachPlayer base time in seconds
     * @param incrementForWhite White increment in seconds
     * @param incrementForBlack Black increment in seconds
     * @return compact display value such as {@code 5+0} or {@code 5+2/3}
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
