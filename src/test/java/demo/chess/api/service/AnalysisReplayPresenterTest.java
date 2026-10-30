package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import demo.chess.api.dto.AnalysisProfilePointDto;
import demo.chess.api.dto.AnalysisReplayStepDto;
import demo.chess.api.dto.BoardDto;
import demo.chess.game.Game;

class AnalysisReplayPresenterTest {

    @Test
    void responseUsesRoundedEvaluationAndDefensiveProfileCopy() {
        GameService games = mock(GameService.class);
        Game game = mock(Game.class);
        BoardDto board = mock(BoardDto.class);
        when(games.getBoardView(game)).thenReturn(board);
        AnalysisReplayPresenter presenter = new AnalysisReplayPresenter(games);
        List<AnalysisProfilePointDto> profile = new ArrayList<>();
        profile.add(new AnalysisProfilePointDto(
                1, "e2", "e4", "e4", 0.12, 0.51, 14));

        AnalysisReplayStepDto result = presenter.present(
                game, true, false, 10, 1, "Engine", profile,
                "e2", "e4", "e4", 0.126, 0.51, 14, null);

        assertEquals(0.13, result.getEvaluation());
        assertEquals(board, result.getBoard());
        assertNotSame(profile, result.getProfile());
        profile.clear();
        assertEquals(1, result.getProfile().size());
    }

    @Test
    void inactiveResponseHasNeutralDefaults() {
        AnalysisReplayStepDto result =
                new AnalysisReplayPresenter(mock(GameService.class)).inactive("inactive");

        assertFalse(result.isActive());
        assertEquals(0.5, result.getBar());
        assertEquals(List.of(), result.getProfile());
        assertEquals("inactive", result.getMessage());
    }
}
