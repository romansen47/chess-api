package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import demo.chess.api.dto.EngineLineDto;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.game.DummyGame;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;

class EngineLineDisplayServiceTest {

    private final GameLoader gameLoader = new GameLoader();
    private final EngineLineDisplayService service = new EngineLineDisplayService();

    @Test
    void displaysCheckSuffixInEngineVariation() throws Exception {
        DummyGame game = Simulation.createDummySimulation();
        gameLoader.loadGame(List.of("e2e4", "e7e5", "g1f3", "d7d6"), game);

        EngineLineDto dto = service.toDto(
                game,
                new EngineLine(0.4, 12, null, "f1b5 c8d7"));

        assertEquals("♗b5+ ♝d7", dto.getMoves());
        assertEquals(3, dto.getPositions().size());
        assertEquals(2, dto.getMoveArrows().size());
        assertEquals("f1", dto.getMoveArrows().get(0).from());
        assertEquals("b5", dto.getMoveArrows().get(0).to());
    }

    @Test
    void castlingArrowUsesKingDestinationInsteadOfUciRookSquare() throws Exception {
        DummyGame game = Simulation.createDummySimulation();
        gameLoader.loadGame(
                List.of("g1f3", "g8f6", "e2e4", "e7e5", "f1e2", "f8e7"),
                game);

        EngineLineDto dto = service.toDto(
                game,
                new EngineLine(0.3, 12, null, "e1h1"));

        assertEquals(1, dto.getMoveArrows().size());
        assertEquals("e1", dto.getMoveArrows().get(0).from());
        assertEquals("g1", dto.getMoveArrows().get(0).to());
    }

    @Test
    void displaysCheckmateSuffixInEngineVariation() throws Exception {
        DummyGame game = Simulation.createDummySimulation();
        gameLoader.loadGame(List.of("f2f3", "e7e5", "g2g4"), game);

        EngineLineDto dto = service.toDto(
                game,
                new EngineLine(-99.0, 8, 1, "d8h4"));

        assertEquals("♛h4#", dto.getMoves());
        assertEquals(2, dto.getPositions().size());
    }
}
