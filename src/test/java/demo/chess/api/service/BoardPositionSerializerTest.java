package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;

import demo.chess.api.dto.EngineLineDto;
import demo.chess.api.dto.GameSnapshotDto;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;

class BoardPositionSerializerTest {

    private static final String INITIAL_POSITION =
            "rnbqkbnrpppppppp................................PPPPPPPPRNBQKBNR";

    private final GameLoader gameLoader = new GameLoader();

    @Test
    void serializesInitialPositionInFrontendBoardOrder() {
        Simulation game = Simulation.createSimulation();

        assertEquals(INITIAL_POSITION, BoardPositionSerializer.toPositionString(game));
    }

    @Test
    void serializesPositionAfterMoveWithoutChangingBoardOrientation() throws Exception {
        Simulation game = Simulation.createSimulation();
        gameLoader.loadGame(List.of("e2e4"), game);

        assertEquals(
                "rnbqkbnrpppppppp....................P...........PPPP.PPPRNBQKBNR",
                BoardPositionSerializer.toPositionString(game));
    }

    @Test
    void returnsEmptyStringWhenNoGameIsAvailable() {
        assertEquals("", BoardPositionSerializer.toPositionString(null));
    }

    @Test
    void uciGameSnapshotUsesCanonicalBoardRepresentation() throws Exception {
        GameService gameService = new GameService();
        EngineRuntimeSelectionService runtimeSelectionService = mock(EngineRuntimeSelectionService.class);
        UciGameService uciGameService = new UciGameService(gameService, runtimeSelectionService);

        GameSnapshotDto snapshot = uciGameService.getCurrentGameSnapshot();

        assertEquals(INITIAL_POSITION, snapshot.getGame().getPosition());
    }

    @Test
    void analysisVariationFacadeUsesCanonicalBoardRepresentation() {
        UciGameService uciGameService = mock(UciGameService.class);
        AnalysisVariationService variationService = new AnalysisVariationService(uciGameService);

        assertEquals(
                INITIAL_POSITION,
                variationService.toPositionString(Simulation.createSimulation()));
    }

    @Test
    void engineLineDisplayUsesCanonicalBoardRepresentation() {
        EngineLineDisplayService displayService = new EngineLineDisplayService();
        Simulation game = Simulation.createSimulation();

        EngineLineDto dto = displayService.toDto(
                game,
                new EngineLine(0.0, 1, null, ""));

        assertEquals(List.of(INITIAL_POSITION), dto.getPositions());
    }
}
