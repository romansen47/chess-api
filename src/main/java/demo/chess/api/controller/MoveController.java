package demo.chess.api.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.BoardDto;
import demo.chess.api.dto.LegalMoveDto;
import demo.chess.api.dto.MoveRequestDto;
import demo.chess.api.dto.MoveResultDto;
import demo.chess.api.dto.PossibleMovesResponse;
import demo.chess.api.mapper.BoardSquareResolver;
import demo.chess.api.mapper.LegalMoveDtoMapper;
import demo.chess.api.service.GameService;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.notation.UciMoveCodec;

/** REST boundary for board display, legal-move queries and human moves. */
@RestController
@RequestMapping("/api")
public class MoveController {

    private final GameService gameService;

    public MoveController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Returns all legal moves starting on the requested square.
     *
     * <p>The legacy {@code targets} array is retained for frontend
     * compatibility. New clients should prefer the richer {@code moves}
     * descriptors, which expose Chess960 castling geometry explicitly.</p>
     */
    @GetMapping("/possible-moves")
    public ResponseEntity<PossibleMovesResponse> getPossibleMoves(@RequestParam("from") String from)
            throws NoMoveFoundException, IOException {
        Game game = gameService.getCurrentGame();
        Field fromField = BoardSquareResolver.resolve(game.getChessBoard(), from);
        if (fromField == null || fromField.getPiece() == null) {
            return ResponseEntity.ok(new PossibleMovesResponse(from, List.of(), List.of()));
        }

        List<String> targets = new ArrayList<>();
        List<LegalMoveDto> moves = new ArrayList<>();
        for (Move move : game.getPlayer().getValidMoves(game)) {
            if (!sameField(move.getSource(), fromField)) continue;
            LegalMoveDto moveDto = LegalMoveDtoMapper.toDto(game, move);
            targets.add(moveDto.target());
            moves.add(moveDto);
        }
        return ResponseEntity.ok(new PossibleMovesResponse(from, targets, moves));
    }

    @GetMapping("/board")
    public ResponseEntity<BoardDto> getBoard() {
        return ResponseEntity.ok(gameService.getBoardView());
    }

    @PostMapping("/move")
    public ResponseEntity<MoveResultDto> makeMove(@RequestBody MoveRequestDto request) {
        String from = request.getFrom();
        String to = request.getTo();
        String promotion = request.getPromotion();
        if (from == null || to == null) {
            return ResponseEntity.badRequest().body(new MoveResultDto(
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
            return ResponseEntity.ok(result);
        } catch (NoMoveFoundException e) {
            return ResponseEntity.badRequest().body(new MoveResultDto(
                    false, e.getMessage(), from, to, null, null));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(new MoveResultDto(
                    false, "I/O error while applying move", from, to, null, null));
        }
    }

    private boolean sameField(Field a, Field b) {
        return a != null && b != null && a.getFile() == b.getFile() && a.getRank() == b.getRank();
    }
}
