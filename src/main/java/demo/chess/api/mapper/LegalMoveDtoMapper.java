package demo.chess.api.mapper;

import demo.chess.api.dto.LegalMoveDto;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.notation.UciMoveCodec;

/**
 * Translates legal core moves into API move descriptors.
 *
 * <p>This is the single API boundary that knows about the Chess960 castling
 * geometry exposed by the core. Controllers therefore do not need to inspect
 * {@link Castling} implementations or know the Chess960 UCI encoding.</p>
 */
public final class LegalMoveDtoMapper {

    private LegalMoveDtoMapper() {
    }

    /**
     * Maps one legal move to its transport representation.
     *
     * @param game game owning the move and its starting-position context
     * @param move legal move to map
     * @return move DTO
     */
    public static LegalMoveDto toDto(Game game, Move move) {
        if (game == null) throw new IllegalArgumentException("game must not be null");
        if (move == null) throw new IllegalArgumentException("move must not be null");

        String target = name(move.getTarget());
        String uci = UciMoveCodec.encode(game, move);
        if (!(move instanceof Castling castling)) {
            return new LegalMoveDto(target, uci, null, null, null, null);
        }

        return new LegalMoveDto(
                target,
                uci,
                castling.getSide().name(),
                name(castling.getKingTarget()),
                name(castling.getRookSource()),
                name(castling.getRookTarget()));
    }

    private static String name(Field field) {
        return field != null ? field.getName() : null;
    }
}
