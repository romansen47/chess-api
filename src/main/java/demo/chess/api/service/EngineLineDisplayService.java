package demo.chess.api.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.EngineLineDto;
import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.pieces.Piece;
import demo.chess.game.Game;

/**
 * Converts raw UCI engine principal variations into the same display data used
 * by the analysis UI: human-readable move text plus one board snapshot for the
 * starting position and after every move in the variation.
 */
@Service
public class EngineLineDisplayService {

    public EngineLineDto toDto(Game currentGame, EngineLine line) {
        if (line == null) {
            return new EngineLineDto(0.0, 0, null, "", List.of());
        }

        EngineLineDisplayData displayData = convertEngineLine(currentGame, line.getMoves());
        double roundedEvaluation = Math.round(line.getEvaluation() * 100.0) / 100.0;

        return new EngineLineDto(
                roundedEvaluation,
                line.getDepth(),
                line.getMateDistance(),
                displayData.moves,
                displayData.positions);
    }

    private EngineLineDisplayData convertEngineLine(Game currentGame, String uciMoves) {
        if (uciMoves == null || uciMoves.isBlank()) {
            return new EngineLineDisplayData(
                    "",
                    currentGame != null ? List.of(toPositionString(currentGame)) : List.of());
        }

        if (currentGame == null) {
            return new EngineLineDisplayData(uciMoves, List.of());
        }

        try {
            StringBuilder result = new StringBuilder();
            List<String> positions = new ArrayList<>();
            positions.add(toPositionString(currentGame));

            String[] tokens = uciMoves.split("\\s+");
            for (String token : tokens) {
                if (token == null || token.isBlank()) {
                    continue;
                }

                Move move = findMoveByUci(currentGame, token);
                if (move == null) {
                    break;
                }

                String displayMove = toDisplaySan(currentGame, move);
                if (!displayMove.isBlank()) {
                    if (result.length() > 0) {
                        result.append(' ');
                    }
                    result.append(displayMove);
                }

                currentGame.apply(move);
                positions.add(toPositionString(currentGame));
            }

            return new EngineLineDisplayData(
                    result.length() > 0 ? result.toString() : uciMoves,
                    positions);
        } catch (Exception ignored) {
            return new EngineLineDisplayData(
                    uciMoves,
                    List.of(toPositionString(currentGame)));
        }
    }

    private String toPositionString(Game game) {
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

    private char toPositionChar(Piece piece) {
        if (piece == null || piece.getType() == null) {
            return '.';
        }

        char pieceChar;
        switch (piece.getType()) {
            case PAWN:
                pieceChar = 'p';
                break;
            case KNIGHT:
                pieceChar = 'n';
                break;
            case BISHOP:
                pieceChar = 'b';
                break;
            case ROOK:
                pieceChar = 'r';
                break;
            case QUEEN:
                pieceChar = 'q';
                break;
            case KING:
                pieceChar = 'k';
                break;
            default:
                pieceChar = '.';
        }

        return piece.getColor() == Color.WHITE
                ? Character.toUpperCase(pieceChar)
                : pieceChar;
    }

    private Move findMoveByUci(Game game, String uci) {
        if (game == null || uci == null || uci.isBlank()) {
            return null;
        }

        try {
            for (Move candidate : game.getPlayer().getValidMoves(game)) {
                if (uci.equals(candidate.toString())) {
                    return candidate;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private String toDisplaySan(Game game, Move move) {
        if (move == null
                || move.getSource() == null
                || move.getTarget() == null
                || move.getPiece() == null) {
            return "";
        }

        Field source = move.getSource();
        Field target = move.getTarget();
        Piece piece = move.getPiece();

        if (move instanceof Castling) {
            return target.getFile() > source.getFile() ? "0-0" : "0-0-0";
        }

        String piecePrefix = getPiecePrefix(piece);
        String sourceDisambiguation = getSourceDisambiguation(game, move);
        boolean capture = target.getPiece() != null || move instanceof EnPassant;
        String captureMarker = capture ? "x" : "";
        String targetName = target.toString();
        String postFix = "";

        if (piece.getType() == PieceType.PAWN && capture) {
            sourceDisambiguation = source.toString().substring(0, 1);
        }

        if (move instanceof EnPassant) {
            postFix = " e.p.";
        }

        if (move instanceof Promotion) {
            Promotion promotion = (Promotion) move;
            if (promotion.getPromotedPiece() != null) {
                postFix = "=" + getPiecePrefix(promotion.getPromotedPiece());
            }
        }

        return piecePrefix + sourceDisambiguation + captureMarker + targetName + postFix;
    }

    private String getSourceDisambiguation(Game game, Move move) {
        Piece piece = move.getPiece();
        if (piece == null || piece.getType() == PieceType.PAWN || move.getTarget() == null) {
            return "";
        }

        List<Move> competingMoves = new ArrayList<>();
        try {
            for (Move candidate : game.getPlayer().getValidMoves(game)) {
                if (candidate.getPiece() == null
                        || candidate.getSource() == null
                        || candidate.getTarget() == null) {
                    continue;
                }

                if (candidate.getSource().equals(move.getSource())
                        && candidate.getTarget().equals(move.getTarget())) {
                    continue;
                }

                if (candidate.getTarget().equals(move.getTarget())
                        && candidate.getPiece().getType() == piece.getType()) {
                    competingMoves.add(candidate);
                }
            }
        } catch (Exception ignored) {
            return "";
        }

        if (competingMoves.isEmpty()) {
            return "";
        }

        boolean sameFileExists = competingMoves.stream()
                .anyMatch(candidate -> candidate.getSource().getFile() == move.getSource().getFile());
        boolean sameRankExists = competingMoves.stream()
                .anyMatch(candidate -> candidate.getSource().getRank() == move.getSource().getRank());

        if (sameFileExists && sameRankExists) {
            return move.getSource().toString();
        }

        if (sameFileExists) {
            return move.getSource().toString().substring(1, 2);
        }

        return move.getSource().toString().substring(0, 1);
    }

    private String getPiecePrefix(Piece piece) {
        if (piece == null || piece.getType() == null || piece.getType() == PieceType.PAWN) {
            return "";
        }

        return getUnicodeSymbol(piece.getType(), piece.getColor());
    }

    private String getUnicodeSymbol(PieceType pieceType, Color color) {
        if (pieceType == null || color == null) {
            return "";
        }

        switch (color) {
            case WHITE:
                switch (pieceType) {
                    case KING:
                        return "♔";
                    case QUEEN:
                        return "♕";
                    case ROOK:
                        return "♖";
                    case BISHOP:
                        return "♗";
                    case KNIGHT:
                        return "♘";
                    default:
                        return "";
                }
            case BLACK:
                switch (pieceType) {
                    case KING:
                        return "♚";
                    case QUEEN:
                        return "♛";
                    case ROOK:
                        return "♜";
                    case BISHOP:
                        return "♝";
                    case KNIGHT:
                        return "♞";
                    default:
                        return "";
                }
            default:
                return "";
        }
    }

    private static final class EngineLineDisplayData {
        private final String moves;
        private final List<String> positions;

        private EngineLineDisplayData(String moves, List<String> positions) {
            this.moves = moves;
            this.positions = positions;
        }
    }
}
