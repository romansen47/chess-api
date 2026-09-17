package demo.chess.api.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import demo.chess.api.dto.BoardDto;
import demo.chess.api.dto.PieceDto;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.pieces.Piece;
import demo.chess.game.Game;

/**
 * Maps the core board model to the stable API board representation.
 *
 * <p>The mapper deliberately contains no game-lifecycle logic. Its sole
 * responsibility is translating domain objects to DTOs, keeping controllers
 * and services free from board traversal details.</p>
 */
public final class BoardDtoMapper {

    private BoardDtoMapper() {
    }

    /**
     * Creates an API board snapshot from the supplied game.
     *
     * @param game source game
     * @return immutable-by-convention board DTO
     * @throws IllegalArgumentException when {@code game} is {@code null}
     */
    public static BoardDto toDto(Game game) {
        if (game == null) throw new IllegalArgumentException("game must not be null");
        Board board = game.getChessBoard();
        List<PieceDto> pieces = new ArrayList<>();
        for (int file = 1; file <= 8; file++) {
            for (int rank = 1; rank <= 8; rank++) {
                Field field = board.getField(file, rank);
                if (field == null || field.getPiece() == null) continue;
                Piece piece = field.getPiece();
                String color = piece.getColor() != null
                        ? piece.getColor().name().toLowerCase(Locale.ROOT)
                        : "unknown";
                String type = piece.getType() != null
                        ? piece.getType().name().toLowerCase(Locale.ROOT)
                        : "piece";
                pieces.add(new PieceDto(color, type, field.getName()));
            }
        }
        return new BoardDto(pieces);
    }
}
