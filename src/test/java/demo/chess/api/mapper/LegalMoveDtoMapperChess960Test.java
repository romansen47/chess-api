package demo.chess.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import demo.chess.api.dto.LegalMoveDto;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.impl.CastlingImpl;
import demo.chess.definitions.pieces.impl.King;
import demo.chess.definitions.pieces.impl.Rook;
import demo.chess.game.impl.Simulation;

class LegalMoveDtoMapperChess960Test {

    @Test
    void exposesProtocolAndBoardGeometrySeparatelyForChess960Castling() {
        Simulation game = Simulation.createSimulation(ChessStartingPosition.of(0));
        King king = (King) game.getChessBoard().getField(7, 1).getPiece();
        Rook rook = (Rook) game.getChessBoard().getField(8, 1).getPiece();
        Castling castling = new CastlingImpl(king, rook);

        LegalMoveDto dto = LegalMoveDtoMapper.toDto(game, castling);

        assertEquals("h1", dto.target());
        assertEquals("g1h1", dto.uci());
        assertEquals("KING_SIDE", dto.castlingSide());
        assertEquals("g1", dto.kingTo());
        assertEquals("h1", dto.rookFrom());
        assertEquals("f1", dto.rookTo());
    }
}
