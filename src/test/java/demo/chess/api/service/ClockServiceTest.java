package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import demo.chess.api.dto.ClockDto;

/** Direct regression coverage for the clock projection application service. */
class ClockServiceTest {

    private GameService gameService;

    /**
     * Verifies the initial clock projection, time-control formatting and engine labels.
     */
    @Test
    void projectsCurrentGameClockAndRuntimeEngineNames() {
        gameService = new GameService();
        EngineRuntimeSelectionService runtime = mock(EngineRuntimeSelectionService.class);
        when(runtime.getWhitePlayerEngineName()).thenReturn("White Engine");
        when(runtime.getBlackPlayerEngineName()).thenReturn("Black Engine");

        ClockDto clock = new ClockService(gameService, runtime).getClock();

        assertEquals(300, clock.getWhiteTime());
        assertEquals(300, clock.getBlackTime());
        assertEquals("white", clock.getSideToMove());
        assertEquals("5+0", clock.getTimeControl());
        assertEquals("White Engine", clock.getWhitePlayerEngineName());
        assertEquals("Black Engine", clock.getBlackPlayerEngineName());
        assertFalse(clock.isWhiteRunning());
        assertFalse(clock.isBlackRunning());
    }

    /** Stops clocks created by the real game fixture. */
    @AfterEach
    void stopClocks() {
        if (gameService == null || gameService.getCurrentGame() == null) return;
        var white = gameService.getCurrentGame().getWhitePlayer().getChessClock();
        var black = gameService.getCurrentGame().getBlackPlayer().getChessClock();
        if (white != null) white.stop();
        if (black != null) black.stop();
    }
}
