package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import demo.chess.api.dto.AnalysisVariationMoveResultDto;
import demo.chess.api.dto.AnalysisVariationRequestDto;

class AnalysisVariationServiceTest {

    @Test
    void reportsCheckmateForTerminalVariationMove() throws Exception {
        UciGameService uciGameService = mock(UciGameService.class);
        when(uciGameService.getAnalysisMoveListSnapshot()).thenReturn(List.of());

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
}
