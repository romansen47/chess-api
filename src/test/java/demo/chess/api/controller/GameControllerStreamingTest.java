package demo.chess.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import demo.chess.api.service.AnalysisReplayService;
import demo.chess.api.service.ChessDatabaseService;
import demo.chess.api.service.GameLifecycleService;
import demo.chess.api.service.GameService;
import demo.chess.api.service.UciGameService;

class GameControllerStreamingTest {

    @Test
    void servletRequestRejectsSecondGameBeforeImportSideEffects() {
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

                1. e4 e5 1-0

                [Event "Second"]
                [Result "0-1"]

                1. d4 d5 0-1
                """;
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType(MediaType.TEXT_PLAIN_VALUE);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent(pgn.getBytes(StandardCharsets.UTF_8));

        ResponseEntity<?> response = controller.importPgnGame(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of(
                "code", "PGN_MULTIPLE_GAMES",
                "gameCount", 2,
                "earlyAbort", true), response.getBody());
        verify(analysisReplayService, never()).cancel();
        verifyNoInteractions(chessDatabaseService, uciGameService);
    }
}
