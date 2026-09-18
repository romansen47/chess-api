package demo.chess.api.service;

import java.util.List;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.moves.Move;

/**
 * Immutable snapshot of the game selected for analysis.
 *
 * <p>A move history is not sufficient to reconstruct Chess960. The
 * {@link ChessStartingPosition} is therefore part of the context itself and
 * travels with the moves through every replay boundary.</p>
 */
record AnalysisGameContext(
        ChessStartingPosition startingPosition,
        List<Move> moves) {

    AnalysisGameContext {
        startingPosition = startingPosition != null
                ? startingPosition
                : ChessStartingPosition.STANDARD;
        moves = moves == null ? List.of() : List.copyOf(moves);
    }
}
