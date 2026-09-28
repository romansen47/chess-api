package demo.chess.api.dto;

import java.util.List;

/**
 * Persistable PGN annotation attached to one main-line half-move.
 *
 * @param ply one-based half-move number, or zero for an introductory comment
 * @param nag symbolic NAG (!, !!, !?, ?!, ? or ??)
 * @param comment free user comment
 * @param evaluation stored engine evaluation without [%eval ...]
 * @param variations PGN recursive variations without outer parentheses
 * @param clockMillis remaining time after the move, in milliseconds, or null
 * @param elapsedMoveMillis time spent on the move, in milliseconds, or null
 */
public record GameAnnotationDto(
        int ply,
        String nag,
        String comment,
        String evaluation,
        List<String> variations,
        Long clockMillis,
        Long elapsedMoveMillis) {

    public GameAnnotationDto(int ply, String nag, String comment, String evaluation, List<String> variations) {
        this(ply, nag, comment, evaluation, variations, null, null);
    }

    public GameAnnotationDto {
        variations = variations == null ? List.of() : List.copyOf(variations);
    }
}
