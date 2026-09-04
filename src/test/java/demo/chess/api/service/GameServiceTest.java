package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import demo.chess.api.dto.GameSettingsDto;
import demo.chess.api.dto.PieceDto;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.game.Game;

class GameServiceTest {

    private GameService service;

    /**
     * Stops any clock that may have been started by a move executed during a test.
     */
    @AfterEach
    void stopClocks() {
        if (service == null || service.getCurrentGame() == null) {
            return;
        }

        stopClock(service.getCurrentGame().getWhitePlayer().getChessClock());
        stopClock(service.getCurrentGame().getBlackPlayer().getChessClock());
    }

    /**
     * Verifies the default game contract exposed to the frontend.
     */
    @Test
    void defaultGameUsesExpectedSettingsAndInitialBoard() {
        service = new GameService();

        GameSettingsDto settings = service.getGameSettings();

        assertEquals(GameService.DEFAULT_TIME_SECONDS, settings.getTimeForEachPlayerSeconds());
        assertEquals(0, settings.getIncrementForWhiteSeconds());
        assertEquals(0, settings.getIncrementForBlackSeconds());
        assertEquals(0, settings.getAdditionalTimeAfter40MovesSeconds());
        assertEquals("WHITE", settings.getStartingColor());
        assertEquals(0L, settings.getVersion());
        assertEquals(32, service.getBoardView().getPieces().size());
        assertEquals(
                "rnbqkbnrpppppppp................................PPPPPPPPRNBQKBNR",
                service.getCurrentPositionString());
    }

    /**
     * Verifies normalization of user supplied game settings and monotonic settings versions.
     */
    @Test
    void startNewGameNormalizesSettingsAndAdvancesVersion() {
        service = new GameService();
        Game originalGame = service.getCurrentGame();

        GameSettingsDto requested = new GameSettingsDto(
                0,
                -5,
                3,
                -10,
                " black ",
                999L);

        GameSettingsDto normalized = service.startNewGame(requested);

        assertEquals(GameService.DEFAULT_TIME_SECONDS, normalized.getTimeForEachPlayerSeconds());
        assertEquals(0, normalized.getIncrementForWhiteSeconds());
        assertEquals(3, normalized.getIncrementForBlackSeconds());
        assertEquals(0, normalized.getAdditionalTimeAfter40MovesSeconds());
        assertEquals("BLACK", normalized.getStartingColor());
        assertEquals(1L, normalized.getVersion());
        assertNotSame(originalGame, service.getCurrentGame());

        GameSettingsDto second = service.startNewGame();
        assertEquals(2L, second.getVersion());
    }

    /**
     * Verifies that callers cannot mutate the service state through the returned settings DTO.
     */
    @Test
    void gameSettingsAreReturnedAsDefensiveCopies() {
        service = new GameService();

        GameSettingsDto copy = service.getGameSettings();
        copy.setTimeForEachPlayerSeconds(1);
        copy.setStartingColor("BLACK");

        GameSettingsDto unchanged = service.getGameSettings();
        assertEquals(GameService.DEFAULT_TIME_SECONDS, unchanged.getTimeForEachPlayerSeconds());
        assertEquals("WHITE", unchanged.getStartingColor());
    }

    /**
     * Verifies coordinate based legal move selection and rejection of an illegal move.
     */
    @Test
    void appliesLegalCoordinateMoveAndRejectsIllegalMove() throws Exception {
        service = new GameService();

        service.applyMove("E2", "E4", null);

        assertEquals(1, service.getMoveListSnapshot().size());
        assertTrue(service.getBoardView().getPieces().stream().anyMatch(this::isWhitePawnOnE4));
        assertFalse(service.getBoardView().getPieces().stream().anyMatch(piece -> "e2".equals(piece.getSquare())));
        assertThrows(NoMoveFoundException.class, () -> service.applyMove("e2", "e5", null));
    }

    /**
     * Verifies that asynchronous callers cannot apply a move to a game that has already been replaced.
     */
    @Test
    void applyMoveIfCurrentRejectsStaleGameInstance() throws Exception {
        service = new GameService();
        Game staleGame = service.getCurrentGame();
        service.startNewGame();

        assertFalse(service.applyMoveIfCurrent(staleGame, null));
    }

    /**
     * Returns whether the DTO represents the white pawn expected after e2-e4.
     * @param piece the piece DTO
     * @return true for the white pawn on e4
     */
    private boolean isWhitePawnOnE4(PieceDto piece) {
        return "white".equals(piece.getColor())
                && "pawn".equals(piece.getType())
                && "e4".equals(piece.getSquare());
    }

    /**
     * Stops one chess clock if it is currently active.
     * @param clock the clock to stop
     */
    private void stopClock(demo.chess.definitions.clocks.impl.ChessClock clock) {
        if (clock != null && clock.isStarted() && !clock.isStopped()) {
            clock.stop();
        }
    }
}
