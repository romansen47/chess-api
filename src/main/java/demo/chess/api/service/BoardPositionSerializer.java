package demo.chess.api.service;

import demo.chess.definitions.Color;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.pieces.Piece;
import demo.chess.game.Game;

/**
 * Canonical serializer for the frontend's compact 64-character board format.
 *
 * <p>Ranks are written from 8 to 1 and files from a to h. Empty squares are
 * represented by '.', black pieces by lowercase letters and white pieces by
 * uppercase letters.</p>
 */
final class BoardPositionSerializer {

    private BoardPositionSerializer() {
    }

    static String toPositionString(Game game) {
        if (game == null || game.getChessBoard() == null) {
            return "";
        }

        Board board = game.getChessBoard();
        StringBuilder position = new StringBuilder(64);

        for (int rank = 8; rank >= 1; rank--) {
            for (int file = 1; file <= 8; file++) {
                Field field = board.getField(file, rank);
                Piece piece = field != null ? field.getPiece() : null;
                position.append(toPositionChar(piece));
            }
        }

        return position.toString();
    }

    private static char toPositionChar(Piece piece) {
        if (piece == null || piece.getType() == null) {
            return '.';
        }

        char pieceChar = switch (piece.getType()) {
            case PAWN -> 'p';
            case KNIGHT -> 'n';
            case BISHOP -> 'b';
            case ROOK -> 'r';
            case QUEEN -> 'q';
            case KING -> 'k';
        };

        return piece.getColor() == Color.WHITE
                ? Character.toUpperCase(pieceChar)
                : pieceChar;
    }
}
