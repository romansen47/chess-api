package demo.chess.api.dto;

/**
 * Explicit legal-move descriptor. For Chess960 castling, {@code target} is the
 * original rook square used by the interaction/UCI request, while king/rook
 * destinations expose the actual board transformation.
 */
public record LegalMoveDto(
        String target,
        String uci,
        String castlingSide,
        String kingTo,
        String rookFrom,
        String rookTo) {
}
