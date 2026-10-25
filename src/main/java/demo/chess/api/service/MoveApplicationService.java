package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.BoardDto;
import demo.chess.api.dto.LegalMoveDto;
import demo.chess.api.dto.MoveRequestDto;
import demo.chess.api.dto.MoveResultDto;
import demo.chess.api.dto.PossibleMovesResponse;
import demo.chess.api.mapper.BoardSquareResolver;
import demo.chess.api.mapper.LegalMoveDtoMapper;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.notation.UciMoveCodec;

/**
 * Application boundary for human board interaction.
 *
 * <p>HTTP controllers delegate here instead of traversing core boards or legal
 * move collections themselves. Chess legality remains authoritative in the
 * {@code chess} module; this service only coordinates core calls and maps their
 * results to transport DTOs.</p>
 */
@Service
public class MoveApplicationService {

    private final GameService gameService;

    /**
     * Creates the board-interaction application service.
     *
     * @param gameService owner of the current game lifecycle
     */
    public MoveApplicationService(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Returns the legal moves available from one board square.
     *
     * @param from algebraic source square such as {@code e2}
     * @return compatibility targets and canonical legal-move descriptors
     */
    public PossibleMovesResponse getPossibleMoves(String from) {
        try {
            Game game = gameService.getCurrentGame();
            Field fromField = BoardSquareResolver.resolve(game.getChessBoard(), from);
            if (fromField == null || fromField.getPiece() == null) {
                return new PossibleMovesResponse(from, List.of(), List.of());
            }

            List<String> targets = new ArrayList<>();
            List<LegalMoveDto> moves = new ArrayList<>();
            for (Move move : game.getPlayer().getValidMoves(game)) {
                if (!sameField(move.getSource(), fromField)) continue;
                LegalMoveDto moveDto = LegalMoveDtoMapper.toDto(game, move);
                targets.add(moveDto.target());
                moves.add(moveDto);
            }
            return new PossibleMovesResponse(from, targets, moves);
        } catch (NoMoveFoundException | IOException e) {
            throw new MoveProcessingException("Could not determine legal moves", e);
        }
    }

    /**
     * Returns the current board projection.
     *
     * @return current board DTO
     */
    public BoardDto getBoard() {
        return gameService.getBoardView();
    }

    /**
     * Applies a human move request through the core legal-move resolver.
     *
     * @param request requested source, target and optional promotion
     * @return application outcome including the move-response DTO
     */
    public MoveOutcome makeMove(MoveRequestDto request) {
        String from = request != null ? request.getFrom() : null;
        String to = request != null ? request.getTo() : null;
        String promotion = request != null ? request.getPromotion() : null;
        if (from == null || to == null) {
            return MoveOutcome.invalid(new MoveResultDto(
                    false, "from/to must not be null", from, to, null, null));
        }

        try {
            Game gameBeforeMove = gameService.getCurrentGame();
            Move appliedMove = gameService.applyMove(from, to, promotion);
            String canonicalUci = UciMoveCodec.encode(gameBeforeMove, appliedMove);
            Game game = gameService.getCurrentGame();
            String san = null;
            List<String> sanMoves = game.getSanMoveList();
            if (sanMoves != null && !sanMoves.isEmpty()) {
                san = sanMoves.get(sanMoves.size() - 1);
            }
            String sideToMove = game.getPlayer() != null && game.getPlayer().getColor() != null
                    ? game.getPlayer().getColor().name().toLowerCase(Locale.ROOT)
                    : null;
            MoveResultDto result = new MoveResultDto(
                    true,
                    null,
                    from,
                    to,
                    san,
                    sideToMove,
                    gameService.getCurrentPositionString(),
                    game.getState() != null ? game.getState().name() : null,
                    game.getMoveList().size());
            result.setUci(canonicalUci);
            return MoveOutcome.success(result);
        } catch (NoMoveFoundException e) {
            return MoveOutcome.invalid(new MoveResultDto(
                    false, e.getMessage(), from, to, null, null));
        } catch (IOException e) {
            return MoveOutcome.failure(new MoveResultDto(
                    false, "I/O error while applying move", from, to, null, null));
        }
    }

    private boolean sameField(Field left, Field right) {
        return left != null && right != null
                && left.getFile() == right.getFile()
                && left.getRank() == right.getRank();
    }

    /** Result category used by the HTTP adapter to choose a status code. */
    public enum MoveOutcomeType {
        SUCCESS,
        INVALID_REQUEST,
        INTERNAL_ERROR
    }

    /**
     * Application result independent of HTTP response types.
     *
     * @param type result category
     * @param result move-response payload
     */
    public record MoveOutcome(MoveOutcomeType type, MoveResultDto result) {
        static MoveOutcome success(MoveResultDto result) {
            return new MoveOutcome(MoveOutcomeType.SUCCESS, result);
        }

        static MoveOutcome invalid(MoveResultDto result) {
            return new MoveOutcome(MoveOutcomeType.INVALID_REQUEST, result);
        }

        static MoveOutcome failure(MoveResultDto result) {
            return new MoveOutcome(MoveOutcomeType.INTERNAL_ERROR, result);
        }
    }

    /** Signals an unexpected failure while projecting legal moves. */
    public static final class MoveProcessingException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        MoveProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
