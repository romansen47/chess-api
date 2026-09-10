package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import demo.chess.definitions.engines.impl.NoMoveFoundException;

class UciGameServiceTimeResultTest {

    @Test
    void pgnResultUsesTimedOutColorOwnedByCore() throws Exception {
        GameService gameService = new GameService();
        EngineRuntimeSelectionService runtimeSelectionService = mock(EngineRuntimeSelectionService.class);
        UciGameService service = new UciGameService(gameService, runtimeSelectionService);

        gameService.getCurrentGame().getWhitePlayer().getChessClock().setTargetTimeMillis(0L);

        try {
            gameService.applyMove("e2", "e4", null);
        } catch (NoMoveFoundException ignored) {
            // The relevant assertion is the core-owned timeout result below.
        }

        String pgn = service.exportGame(false, false);

        assertTrue(pgn.contains("[Result \"0-1\"]"));
        assertTrue(pgn.trim().endsWith("0-1"));
    }
}
