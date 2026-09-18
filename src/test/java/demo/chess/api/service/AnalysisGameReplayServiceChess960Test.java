package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.moves.Move;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

class AnalysisGameReplayServiceChess960Test {

    @Test
    void historicalReplayNeverFallsBackToClassicalPosition518() throws Exception {
        UciGameService uciGameService = mock(UciGameService.class);
        ChessStartingPosition startingPosition = ChessStartingPosition.of(0);
        Simulation original = Simulation.createSimulation(startingPosition);
        List<Move> moves = new ArrayList<>();

        for (String uci : List.of("b2b3", "c7c5", "a1b2")) {
            Move move = LegalMoveResolver.resolveUci(original, uci);
            original.apply(move);
            moves.add(move);
        }

        when(uciGameService.getAnalysisGameContext()).thenReturn(
                new AnalysisGameContext(startingPosition, moves));

        AnalysisGameReplayService replayService = new AnalysisGameReplayService(uciGameService);
        Simulation replay = replayService.createPositionAtPly(3);

        assertEquals(0, replay.getStartingPosition().getId());
        assertEquals(3, replay.getMoveList().size());
        assertEquals("a1", replay.getMoveList().get(2).getSource().getName());
        assertEquals("b2", replay.getMoveList().get(2).getTarget().getName());
    }
}
