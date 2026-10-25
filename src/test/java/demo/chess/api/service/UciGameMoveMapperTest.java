package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import demo.chess.api.dto.UciGameMoveDto;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

/** Direct regression coverage for replay-to-DTO move projection. */
class UciGameMoveMapperTest {

    /**
     * Verifies that replay projection preserves ply order, UCI notation and SAN.
     *
     * @throws Exception when the fixture move sequence cannot be applied
     */
    @Test
    void mapsReplayableMoveHistoryToStablePerPlyDtos() throws Exception {
        Simulation game = Simulation.createSimulation();
        game.apply(LegalMoveResolver.resolveUci(game, "e2e4"));
        game.apply(LegalMoveResolver.resolveUci(game, "e7e5"));

        List<UciGameMoveDto> moves = UciGameMoveMapper.toDtos(game.getMoveList());

        assertEquals(2, moves.size());
        assertEquals(1, moves.get(0).getPly());
        assertEquals("e2e4", moves.get(0).getUci());
        assertEquals("e4", moves.get(0).getSan());
        assertEquals(2, moves.get(1).getPly());
        assertEquals("e7e5", moves.get(1).getUci());
        assertEquals("e5", moves.get(1).getSan());
    }
}
