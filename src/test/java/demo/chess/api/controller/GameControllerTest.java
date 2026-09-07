package demo.chess.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

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
     * local database and still opens the same, unchanged PGN through the analysis import path.
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

    /**
     * Verifies that a multi-game PGN is rejected before any analysis state or
     * database content is changed.
     */
    @Test
    void multiplePgnGamesAreRejectedWithoutSideEffects() throws Exception {
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
                [Event "First"]
                [Result "1-0"]

                1. e4 e5 2. Nf3 Nc6 1-0

                [Event "Second"]
                [Result "0-1"]

                1. d4 d5 2. c4 e6 0-1
                """;

        ResponseEntity<?> response = controller.importPgnGame(pgn);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("code", "PGN_MULTIPLE_GAMES", "gameCount", 2), response.getBody());
        verify(analysisReplayService, never()).cancel();
        verifyNoInteractions(chessDatabaseService, uciGameService);
    }

    /**
     * Verifies that blank input is rejected before any analysis or database side effects.
     */
    @Test
    void emptyPgnIsRejectedWithoutSideEffects() throws Exception {
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

        ResponseEntity<?> response = controller.importPgnGame("   \n\t");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("code", "PGN_NO_GAME", "gameCount", 0), response.getBody());
        verify(analysisReplayService, never()).cancel();
        verifyNoInteractions(chessDatabaseService, uciGameService);
    }
}
