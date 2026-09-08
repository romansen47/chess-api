package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.AnalysisVariationMoveResultDto;
import demo.chess.api.dto.AnalysisVariationRequestDto;
import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.pieces.Piece;
import demo.chess.game.impl.Simulation;

/**
 * Reconstructs temporary analysis variations from an original anchor ply and
 * an ordered list of UCI moves. No variation state is kept on the server.
 */
@Service
public class AnalysisVariationService {

    private final UciGameService uciGameService;

    /**
     * Creates a new AnalysisVariationService instance.
     * @param uciGameService the source of the original analysis game
     */
    public AnalysisVariationService(UciGameService uciGameService) {
        this.uciGameService = uciGameService;
    }

    /**
     * Reconstructs the complete variation position.
     * @param anchorPly original-game ply from which the variation starts
     * @param variationMoves UCI moves after the anchor
     * @return reconstructed simulation
     */
    public Simulation createVariationGame(int anchorPly, List<String> variationMoves)
            throws NoMoveFoundException, IOException {
        List<Move> originalMoves = uciGameService.getAnalysisMoveListSnapshot();
        if (anchorPly < 0 || anchorPly > originalMoves.size()) {
            throw new IllegalArgumentException(
                    "Analysis anchor ply must be between 0 and " + originalMoves.size()
                            + ", got " + anchorPly);
        }

        Simulation simulation = Simulation.createSimulation();
        for (int index = 0; index < anchorPly; index++) {
            Move originalMove = originalMoves.get(index);
            Move replayMove = simulation.getPlayer().getMoveInSimulation(simulation, originalMove);
            simulation.apply(replayMove);
        }

        for (String uci : safeMoves(variationMoves)) {
            Move variationMove = findLegalMoveByUci(simulation, uci);
            simulation.apply(variationMove);
        }

        return simulation;
    }

    /**
     * Returns legal target squares for a source square in the reconstructed
     * variation position.
     * @param request variation request
     * @return legal target squares
     */
    public List<String> getPossibleTargets(AnalysisVariationRequestDto request)
            throws NoMoveFoundException, IOException {
        if (request == null || request.getFrom() == null || request.getFrom().isBlank()) {
            return List.of();
        }

        Simulation simulation = createVariationGame(request.getAnchorPly(), request.getMoves());
        String from = request.getFrom().trim().toLowerCase(Locale.ROOT);
        List<String> targets = new ArrayList<>();

        for (Move move : simulation.getPlayer().getValidMoves(simulation)) {
            if (move.getSource() != null
                    && move.getTarget() != null
                    && from.equalsIgnoreCase(move.getSource().getName())) {
                targets.add(move.getTarget().getName());
            }
        }

        return targets;
    }

    /**
     * Applies one legal move to a reconstructed variation.
     * @param request variation plus requested move
     * @return resulting variation position
     */
    public AnalysisVariationMoveResultDto applyMove(AnalysisVariationRequestDto request)
            throws NoMoveFoundException, IOException {
        if (request == null || request.getFrom() == null || request.getTo() == null) {
            throw new NoMoveFoundException("from/to must not be null");
        }

        Simulation simulation = createVariationGame(request.getAnchorPly(), request.getMoves());
        Move selected = findLegalMove(
                simulation,
                request.getFrom(),
                request.getTo(),
                request.getPromotion());
        String uci = selected.toString();
        simulation.apply(selected);

        String sideToMove = simulation.getPlayer() != null && simulation.getPlayer().getColor() != null
                ? simulation.getPlayer().getColor().name().toLowerCase(Locale.ROOT)
                : null;
        String gameState = simulation.getState() != null ? simulation.getState().name() : null;

        return new AnalysisVariationMoveResultDto(
                true,
                null,
                request.getFrom(),
                request.getTo(),
                uci,
                sideToMove,
                toPositionString(simulation),
                gameState);
    }

    private Move findLegalMoveByUci(Simulation simulation, String rawUci)
            throws NoMoveFoundException, IOException {
        String wanted = rawUci != null ? rawUci.trim().toLowerCase(Locale.ROOT) : "";
        if (wanted.length() < 4) {
            throw new NoMoveFoundException("Invalid variation UCI move: " + rawUci);
        }

        for (Move candidate : simulation.getPlayer().getValidMoves(simulation)) {
            if (candidate != null && wanted.equals(candidate.toString().toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }

        throw new NoMoveFoundException("No legal variation move for UCI " + rawUci);
    }

    private Move findLegalMove(Simulation simulation, String from, String to, String promotion)
            throws NoMoveFoundException, IOException {
        String fromNorm = from.trim().toLowerCase(Locale.ROOT);
        String toNorm = to.trim().toLowerCase(Locale.ROOT);
        String promotionLabel = normalizePromotion(promotion);
        List<Move> matches = new ArrayList<>();

        for (Move candidate : simulation.getPlayer().getValidMoves(simulation)) {
            if (candidate == null || candidate.getSource() == null || candidate.getTarget() == null) {
                continue;
            }
            if (!fromNorm.equalsIgnoreCase(candidate.getSource().getName())
                    || !toNorm.equalsIgnoreCase(candidate.getTarget().getName())) {
                continue;
            }
            if (promotionLabel != null) {
                if (!(candidate instanceof Promotion)) {
                    continue;
                }
                Promotion promotionMove = (Promotion) candidate;
                if (promotionMove.getPromotedPiece() == null
                        || promotionMove.getPromotedPiece().getType() == null
                        || !promotionLabel.equals(promotionMove.getPromotedPiece().getType().label)) {
                    continue;
                }
            }
            matches.add(candidate);
        }

        if (matches.isEmpty()) {
            throw new NoMoveFoundException("No legal variation move for " + from + " -> " + to);
        }

        return matches.get(0);
    }

    private String normalizePromotion(String promotion) {
        if (promotion == null || promotion.isBlank()) {
            return null;
        }

        String normalized = promotion.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "q", "queen" -> "q";
            case "r", "rook" -> "r";
            case "b", "bishop" -> "b";
            case "n", "knight" -> "n";
            default -> normalized;
        };
    }

    private List<String> safeMoves(List<String> moves) {
        return moves != null ? moves : List.of();
    }

    /**
     * Converts a reconstructed game into the frontend's compact 64-character
     * board representation.
     * @param simulation source game
     * @return board representation
     */
    public String toPositionString(Simulation simulation) {
        Board board = simulation.getChessBoard();
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
        PieceType type = piece.getType();
        switch (type) {
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
                break;
        }

        return piece.getColor() == Color.WHITE ? Character.toUpperCase(pieceChar) : pieceChar;
    }
}
