package demo.chess.api.dto;

/**
 * Canonical API description of one legal move.
 *
 * <p>{@code target} is the interaction target accepted by the existing move
 * endpoint. For a Chess960 castling move this remains the original rook square
 * for backwards compatibility with the core move contract. The actual board
 * transformation is explicit in {@code kingTo}, {@code rookFrom} and
 * {@code rookTo}; {@code uci} contains the protocol-correct UCI move.</p>
 *
 * @param target API interaction target
 * @param uci canonical UCI representation
 * @param castlingSide {@code KING_SIDE}/{@code QUEEN_SIDE}, or {@code null}
 * @param kingTo final king square for castling, otherwise {@code null}
 * @param rookFrom original castling-rook square, otherwise {@code null}
 * @param rookTo final rook square for castling, otherwise {@code null}
 */
public record LegalMoveDto(
        String target,
        String uci,
        String castlingSide,
        String kingTo,
        String rookFrom,
        String rookTo) {
}
