package demo.chess.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import demo.chess.api.dto.UciGameDto;
import demo.chess.api.service.AnalysisReplayService;
import demo.chess.api.service.ChessDatabaseService;
import demo.chess.api.service.GameLifecycleService;
import demo.chess.api.service.GameService;
import demo.chess.api.service.UciGameService;

class GameControllerTest {

    /**
     * Verifies that the legacy single-game PGN endpoint stores the game in the
     * local database and still opens the same PGN through the analysis import path.
     */
    @Test
    void singlePgnImportStoresAndLoadsGame() throws Exception {
        GameService gameService = mock(GameService.class);
        GameLifecycleService gameLifecycleService = mock(GameLifecycleService.class);
        UciGameService uciGameService = mock(UciGameService.class);
        AnalysisReplayService analysisReplayService = mock(AnalysisReplayService.class);
        ChessDatabaseService chessDatabaseService = mock(ChessDatabaseService.class);
        GameController controller = new GameController(
                gameService,
                gameLifecycleService,
                uciGameService,
                analysisReplayService,
                chessDatabaseService);

        String pgn = """
                [Event "Single Import"]
                [White "White"]
                [Black "Black"]
                [Result "*"]

                1. e4 e5 *
                """;
        UciGameDto importedGame = new UciGameDto();
        when(uciGameService.importGame(pgn)).thenReturn(importedGame);

        ResponseEntity<?> response = controller.importPgnGame(pgn);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(importedGame, response.getBody());
        verify(analysisReplayService).cancel();

        InOrder importOrder = inOrder(chessDatabaseService, uciGameService);
        importOrder.verify(chessDatabaseService).importSingleGame(pgn);
        importOrder.verify(uciGameService).importGame(pgn);
    }
}
