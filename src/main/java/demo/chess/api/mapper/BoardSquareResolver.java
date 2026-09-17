package demo.chess.api.mapper;

import java.util.Locale;

import demo.chess.definitions.board.Board;
import demo.chess.definitions.fields.Field;

/** Utility for translating algebraic API square names such as {@code e4}. */
public final class BoardSquareResolver {

    private BoardSquareResolver() {
    }

    /**
     * Resolves a two-character algebraic square name against a board.
     * Invalid input yields {@code null}; callers can expose that as an empty
     * result or a request-validation error as appropriate for the endpoint.
     *
     * @param board source board
     * @param square algebraic square name
     * @return resolved field or {@code null}
     */
    public static Field resolve(Board board, String square) {
        if (board == null || square == null || square.length() != 2) return null;
        String normalized = square.toLowerCase(Locale.ROOT);
        char file = normalized.charAt(0);
        char rank = normalized.charAt(1);
        if (file < 'a' || file > 'h' || rank < '1' || rank > '8') return null;
        return board.getField(file - 'a' + 1, rank - '1' + 1);
    }
}
