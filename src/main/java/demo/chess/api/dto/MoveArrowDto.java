package demo.chess.api.dto;

/**
 * Geometric move trajectory for board overlays.
 *
 * <p>For Chess960 castling the trajectory is deliberately king source to
 * king target, while the UCI protocol itself continues to use king source to
 * rook source.</p>
 */
public record MoveArrowDto(String from, String to) {
}
