package demo.chess.api.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.EngineLineDto;
import demo.chess.api.dto.MoveArrowDto;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.notation.PgnNotation;

/**
 * Converts raw UCI engine principal variations into analysis display data.
 *
 * <p>This service owns traversal and board-snapshot creation only. Chess
 * notation itself is delegated to the canonical formatter in the chess core.</p>
 */
@Service
public class EngineLineDisplayService {

    /**
     * Converts one engine line to its frontend DTO.
     * @param currentGame disposable replay position at the start of the line
     * @param line engine line
     * @return display DTO
     */
    public EngineLineDto toDto(Game currentGame, EngineLine line) {
        if (line == null) {
            return new EngineLineDto(0.0, 0, null, "", List.of());
        }

        EngineLineDisplayData displayData = convertEngineLine(currentGame, line.getMoves());
        double roundedEvaluation = Math.round(line.getEvaluation() * 100.0) / 100.0;

        EngineLineDto result = new EngineLineDto(
                roundedEvaluation,
                line.getDepth(),
                line.getMateDistance(),
                displayData.moves,
                displayData.positions);
        result.setMoveArrows(displayData.moveArrows);
        return result;
    }

    private EngineLineDisplayData convertEngineLine(Game currentGame, String uciMoves) {
        if (uciMoves == null || uciMoves.isBlank()) {
            return new EngineLineDisplayData(
                    "",
                    currentGame != null ? List.of(BoardPositionSerializer.toPositionString(currentGame)) : List.of(),
                    List.of());
        }

        if (currentGame == null) {
            return new EngineLineDisplayData(uciMoves, List.of(), List.of());
        }

        try {
            StringBuilder result = new StringBuilder();
            List<String> positions = new ArrayList<>();
            List<MoveArrowDto> moveArrows = new ArrayList<>();
            positions.add(BoardPositionSerializer.toPositionString(currentGame));

            for (String token : uciMoves.split("\\s+")) {
                if (token == null || token.isBlank()) {
                    continue;
                }

                Move move = findMoveByUci(currentGame, token);
                if (move == null) {
                    break;
                }

                moveArrows.add(toMoveArrow(move));
                String displayMove = PgnNotation.toDisplayNotationAndApply(currentGame, move);
                if (!displayMove.isBlank()) {
                    if (result.length() > 0) {
                        result.append(' ');
                    }
                    result.append(displayMove);
                }
                positions.add(BoardPositionSerializer.toPositionString(currentGame));
            }

            return new EngineLineDisplayData(
                    result.length() > 0 ? result.toString() : uciMoves,
                    positions,
                    moveArrows);
        } catch (Exception ignored) {
            return new EngineLineDisplayData(
                    uciMoves,
                    List.of(BoardPositionSerializer.toPositionString(currentGame)),
                    List.of());
        }
    }

    private MoveArrowDto toMoveArrow(Move move) {
        if (move == null || move.getSource() == null) {
            return null;
        }
        String from = move.getSource().getName();
        String to;
        if (move instanceof Castling castling && castling.getKingTarget() != null) {
            to = castling.getKingTarget().getName();
        } else {
            to = move.getTarget() != null ? move.getTarget().getName() : null;
        }
        return to != null ? new MoveArrowDto(from, to) : null;
    }

    private Move findMoveByUci(Game game, String uci) {
        if (game == null || uci == null || uci.isBlank()) {
            return null;
        }

        try {
            return LegalMoveResolver.resolveUci(game, uci);
        } catch (Exception ignored) {
            return null;
        }
    }


    private static final class EngineLineDisplayData {
        private final String moves;
        private final List<String> positions;
        private final List<MoveArrowDto> moveArrows;

        private EngineLineDisplayData(
                String moves,
                List<String> positions,
                List<MoveArrowDto> moveArrows) {
            this.moves = moves;
            this.positions = positions;
            this.moveArrows = moveArrows == null
                    ? List.of()
                    : moveArrows.stream().filter(java.util.Objects::nonNull).toList();
        }
    }
}
