package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import demo.chess.api.dto.GameSettingsDto;

class GameLifecycleServiceTest {

    @Test
    void stopsOldGameEnginesBeforeCreatingReplacementGame() {
        GameService gameService = mock(GameService.class);
        EngineLifecycleCoordinator coordinator = mock(EngineLifecycleCoordinator.class);
        GameSettingsDto requested = new GameSettingsDto();
        GameSettingsDto applied = new GameSettingsDto();
        when(gameService.startNewGame(requested)).thenReturn(applied);

        GameLifecycleService service = new GameLifecycleService(gameService, coordinator);

        assertSame(applied, service.startNewGame(requested));

        InOrder order = inOrder(coordinator, gameService);
        order.verify(coordinator).stopGameScopedEngines();
        order.verify(gameService).startNewGame(requested);
    }

    @Test
    void preparesImportedGameReplacementWithoutCreatingNewLiveGame() {
        GameService gameService = mock(GameService.class);
        EngineLifecycleCoordinator coordinator = mock(EngineLifecycleCoordinator.class);
        GameLifecycleService service = new GameLifecycleService(gameService, coordinator);

        service.prepareForGameReplacement();

        org.mockito.Mockito.verify(coordinator).stopGameScopedEngines();
        org.mockito.Mockito.verifyNoInteractions(gameService);
    }
}
