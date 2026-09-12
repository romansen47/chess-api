package demo.chess.api.dto;

import java.util.List;

/**
 * Persistable PGN annotation attached to one main-line half-move.
 *
 * @param ply one-based half-move number
 * @param nag symbolic NAG (!, !!, !?, ?!, ? or ??)
 * @param comment free user comment
 * @param evaluation stored engine evaluation without [%eval ...]
 * @param variations PGN recursive variations without outer parentheses
 */
public record GameAnnotationDto(
        int ply,
        String nag,
        String comment,
        String evaluation,
        List<String> variations) {

    public GameAnnotationDto {
        variations = variations == null ? List.of() : List.copyOf(variations);
    }
}
