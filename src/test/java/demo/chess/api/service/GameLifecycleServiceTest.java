package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import demo.chess.api.dto.GameSettingsDto;
import demo.chess.api.dto.OpeningDto;

class GameLifecycleServiceTest {

    @Test
    void stopsOldGameEnginesBeforeCreatingReplacementGame() {
        GameService gameService = mock(GameService.class);
        EngineLifecycleCoordinator coordinator = mock(EngineLifecycleCoordinator.class);
        GameSettingsDto requested = new GameSettingsDto();
        GameSettingsDto applied = new GameSettingsDto();
        when(gameService.startNewGame(requested)).thenReturn(applied);

        OpeningLibraryService openingLibraryService = mock(OpeningLibraryService.class);
        GameLifecycleService service = new GameLifecycleService(
                gameService, coordinator, openingLibraryService);

        assertSame(applied, service.startNewGame(requested));

        InOrder order = inOrder(coordinator, gameService);
        order.verify(coordinator).stopGameScopedEngines();
        order.verify(gameService).startNewGame(requested);
    }

    @Test
    void resolvesAssignedOpeningBeforeStartingGame() {
        GameService gameService = mock(GameService.class);
        EngineLifecycleCoordinator coordinator = mock(EngineLifecycleCoordinator.class);
        OpeningLibraryService openingLibraryService = mock(OpeningLibraryService.class);
        GameSettingsDto requested = new GameSettingsDto();
        requested.setOpeningId("opening-1");

        OpeningDto opening = new OpeningDto();
        opening.setId("opening-1");
        opening.setStartingPositionId(518);
        opening.setMoves(java.util.List.of("e2e4", "c7c5"));
        opening.setPlayable(true);
        when(openingLibraryService.getOpening("opening-1")).thenReturn(opening);

        GameSettingsDto applied = new GameSettingsDto();
        when(gameService.startNewGame(
                org.mockito.ArgumentMatchers.same(requested),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(opening.getMoves())))
                .thenReturn(applied);

        GameLifecycleService service = new GameLifecycleService(
                gameService, coordinator, openingLibraryService);

        assertSame(applied, service.startNewGame(requested));

        InOrder order = inOrder(coordinator, openingLibraryService, gameService);
        order.verify(coordinator).stopGameScopedEngines();
        order.verify(openingLibraryService).getOpening("opening-1");
        order.verify(gameService).startNewGame(
                org.mockito.ArgumentMatchers.same(requested),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(opening.getMoves()));
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
