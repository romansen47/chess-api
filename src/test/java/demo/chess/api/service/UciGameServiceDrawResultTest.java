package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import demo.chess.definitions.states.State;

class UciGameServiceDrawResultTest {

    @Test
    void pgnUsesDrawResultForInsufficientMaterial() throws Exception {
        GameService gameService = new GameService();
        EngineRuntimeSelectionService runtimeSelectionService = mock(EngineRuntimeSelectionService.class);
        UciGameService service = new UciGameService(gameService, runtimeSelectionService);

        gameService.getCurrentGame().setState(State.DRAW_BY_INSUFFICIENT_MATERIAL);

        String pgn = service.exportGame(false, false);

        assertTrue(pgn.contains("[Result \"1/2-1/2\"]"));
        assertTrue(pgn.trim().endsWith("1/2-1/2"));
    }
}
