package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import demo.chess.api.dto.BrowserLineDisplayRequestDto;
import demo.chess.api.dto.EngineLineDto;
import demo.chess.definitions.ChessStartingPosition;

class BrowserLineDisplayServiceTest {

    private final BrowserLineDisplayService service =
            new BrowserLineDisplayService(new EngineLineDisplayService());

    @Test
    void reconstructsPositionAndUsesCanonicalLineDisplay() throws Exception {
        BrowserLineDisplayRequestDto request = new BrowserLineDisplayRequestDto(
                ChessStartingPosition.STANDARD.initialFen(),
                List.of("e2e4", "e7e5", "g1f3", "d7d6"),
                List.of(new EngineLineDto(0.4, 18, null, "f1b5 c8d7")));

        EngineLineDto result = service.enrich(request).get(0);

        assertEquals(0.4, result.getEval());
        assertEquals(18, result.getDepth());
        assertEquals("♗b5+ ♝d7", result.getMoves());
        assertEquals(3, result.getPositions().size());
        assertEquals("f1", result.getMoveArrows().get(0).from());
        assertEquals("b5", result.getMoveArrows().get(0).to());
    }

    @Test
    void preservesChess960CastlingArrowSemantics() throws Exception {
        BrowserLineDisplayRequestDto request = new BrowserLineDisplayRequestDto(
                ChessStartingPosition.STANDARD.initialFen(),
                List.of("g1f3", "g8f6", "e2e4", "e7e5", "f1e2", "f8e7"),
                List.of(new EngineLineDto(0.3, 15, null, "e1h1")));

        EngineLineDto result = service.enrich(request).get(0);

        assertEquals("e1", result.getMoveArrows().get(0).from());
        assertEquals("g1", result.getMoveArrows().get(0).to());
        assertEquals(2, result.getPositions().size());
    }

    @Test
    void rejectsMoreLinesThanTheBrowserEngineCanDisplay() {
        List<EngineLineDto> lines = new ArrayList<>();
        for (int index = 0; index < 7; index++) {
            lines.add(new EngineLineDto(0, 1, "e2e4"));
        }
        BrowserLineDisplayRequestDto request = new BrowserLineDisplayRequestDto(
                ChessStartingPosition.STANDARD.initialFen(),
                List.of(),
                lines);

        assertThrows(IllegalArgumentException.class, () -> service.enrich(request));
    }
}
