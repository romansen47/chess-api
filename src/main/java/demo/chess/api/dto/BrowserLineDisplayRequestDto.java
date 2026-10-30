package demo.chess.api.dto;

import java.util.List;

/**
 * Raw browser-engine lines attached to the exact position they analyse.
 *
 * <p>The initial FEN and played UCI moves make this request independent from
 * the live game, which may already have advanced when the response arrives.</p>
 *
 * @param initialFen canonical Chess960 initial FEN
 * @param positionMoves played UCI moves leading to the analysed position
 * @param lines raw engine lines to enrich for display
 */
public record BrowserLineDisplayRequestDto(
        String initialFen,
        List<String> positionMoves,
        List<EngineLineDto> lines) {
}
