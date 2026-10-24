package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import demo.chess.api.dto.AnalysisVariationMoveResultDto;
import demo.chess.api.dto.AnalysisVariationRequestDto;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.moves.Move;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

class AnalysisVariationServiceTest {

    @Test
    void reportsCheckmateForTerminalVariationMove() throws Exception {
        UciGameService uciGameService = mock(UciGameService.class);
        when(uciGameService.getAnalysisGameContext()).thenReturn(
                new AnalysisGameContext(ChessStartingPosition.STANDARD, List.of()));

        AnalysisVariationService service = new AnalysisVariationService(uciGameService);
        AnalysisVariationRequestDto request = new AnalysisVariationRequestDto();
        request.setAnchorPly(0);
        request.setMoves(List.of(
                "h2h4", "g7g5", "h4g5", "g8h6",
                "g5h6", "f8g7", "h6g7", "c7c6"));
        request.setFrom("g7");
        request.setTo("h8");
        request.setPromotion("rook");

        AnalysisVariationMoveResultDto result = service.applyMove(request);

        assertEquals("g7h8r", result.getUci());
        assertEquals("BLACK_MATED", result.getGameState());
    }

    @Test
    void reconstructsChess960VariationFromOriginalStartingPosition() throws Exception {
        UciGameService uciGameService = mock(UciGameService.class);
        ChessStartingPosition startingPosition = ChessStartingPosition.of(0);
        Simulation original = Simulation.createSimulation(startingPosition);
        List<Move> originalMoves = new ArrayList<>();

        for (String uci : List.of("b2b3", "c7c5")) {
            Move move = LegalMoveResolver.resolveUci(original, uci);
            original.apply(move);
            originalMoves.add(move);
        }

        when(uciGameService.getAnalysisGameContext()).thenReturn(
                new AnalysisGameContext(startingPosition, originalMoves));

        AnalysisVariationService service = new AnalysisVariationService(uciGameService);
        Simulation variation = service.createVariationGame(2, List.of("a1b2"));

        assertEquals(0, variation.getStartingPosition().getId());
        assertEquals(3, variation.getMoveList().size());
        assertEquals("b2", variation.getMoveList().get(2).getTarget().getName());
    }
}
