package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import demo.chess.api.dto.UciGameMoveDto;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.game.impl.Simulation;
import demo.chess.notation.PgnNotation;
import demo.chess.notation.UciMoveCodec;

/**
 * Replays core moves to produce stable per-ply API representations.
 *
 * <p>The mapper always recreates the game from the move list's own starting
 * position. This is essential for Chess960 because neither SAN nor UCI can be
 * interpreted correctly from the move sequence alone.</p>
 */
final class UciGameMoveMapper {

    private UciGameMoveMapper() {
    }

    /** Converts a replayable move list into ordered per-ply DTOs. */
    static List<UciGameMoveDto> toDtos(MoveList originalMoves)
            throws NoMoveFoundException, IOException {
        if (originalMoves == null) return List.of();
        ChessStartingPosition startingPosition = originalMoves.getStartingPosition() != null
                ? originalMoves.getStartingPosition()
                : ChessStartingPosition.STANDARD;
        Simulation replayGame = Simulation.createSimulation(startingPosition);
        List<UciGameMoveDto> result = new ArrayList<>();
        int ply = 0;
        for (Move originalMove : originalMoves) {
            if (originalMove == null) continue;
            ply++;
            Move replayMove = replayGame.getPlayer().getMoveInSimulation(replayGame, originalMove);
            if (replayMove == null) {
                throw new NoMoveFoundException("Could not map replay move: " + originalMove);
            }
            String uci = UciMoveCodec.encode(replayGame, replayMove);
            String san = PgnNotation.toDisplayNotationAndApply(replayGame, replayMove);
            result.add(new UciGameMoveDto(
                    ply,
                    uci,
                    san,
                    BoardPositionSerializer.toPositionString(replayGame)));
        }
        return result;
    }
}
