package demo.chess.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import demo.chess.api.dto.BrowserLineDisplayRequestDto;
import demo.chess.api.dto.EngineLineDto;
import demo.chess.api.service.BrowserLineDisplayService;

class BrowserLineDisplayControllerTest {

    @Test
    void returnsEnrichedLinesFromService() throws Exception {
        BrowserLineDisplayService service = mock(BrowserLineDisplayService.class);
        BrowserLineDisplayRequestDto request = request();
        List<EngineLineDto> expected = List.of(new EngineLineDto(0.2, 12, "e4"));
        when(service.enrich(request)).thenReturn(expected);

        ResponseEntity<?> response = new BrowserLineDisplayController(service).enrich(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(service).enrich(request);
    }

    @Test
    void reportsInvalidPositionDataAsBadRequest() throws Exception {
        BrowserLineDisplayService service = mock(BrowserLineDisplayService.class);
        BrowserLineDisplayRequestDto request = request();
        when(service.enrich(request)).thenThrow(new IllegalArgumentException("illegal move"));

        ResponseEntity<?> response = new BrowserLineDisplayController(service).enrich(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("illegal move", response.getBody());
    }

    private BrowserLineDisplayRequestDto request() {
        return new BrowserLineDisplayRequestDto(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w HAha - 0 1",
                List.of(),
                List.of(new EngineLineDto(0.2, 12, "e2e4")));
    }
}
