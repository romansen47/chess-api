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
import demo.chess.api.service.GameService;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.notation.UciMoveCodec;

@RestController
@RequestMapping("/api")
public class MoveController {

    private final GameService gameService;

    public MoveController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/possible-moves")
    public ResponseEntity<PossibleMovesResponse> getPossibleMoves(@RequestParam("from") String from)
            throws NoMoveFoundException, IOException {
        Game game = gameService.getCurrentGame();
        Board board = game.getChessBoard();
        Field fromField = mapSquareToField(board, from);
        if (fromField == null || fromField.getPiece() == null) {
            return ResponseEntity.ok(new PossibleMovesResponse(from, List.of(), List.of()));
        }

        List<String> targets = new ArrayList<>();
        List<LegalMoveDto> moves = new ArrayList<>();
        for (Move move : game.getPlayer().getValidMoves(game)) {
            if (!sameField(move.getSource(), fromField)) {
                continue;
            }
            String target = move.getTarget().getName();
            targets.add(target);
            if (move instanceof Castling castling) {
                moves.add(new LegalMoveDto(
                        target,
                        UciMoveCodec.encode(game, move),
                        castling.getSide().name(),
                        castling.getKingTarget().getName(),
                        castling.getRook().getField().getName(),
                        castling.getRookTarget().getName()));
            } else {
                moves.add(new LegalMoveDto(
                        target,
                        UciMoveCodec.encode(game, move),
                        null,
                        null,
                        null,
                        null));
            }
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

    private Field mapSquareToField(Board board, String square) {
        if (square == null || square.length() != 2) {
            return null;
        }
        square = square.toLowerCase(Locale.ROOT);
        char fileChar = square.charAt(0);
        char rankChar = square.charAt(1);
        if (fileChar < 'a' || fileChar > 'h' || rankChar < '1' || rankChar > '8') {
            return null;
        }
        return board.getField(fileChar - 'a' + 1, rankChar - '1' + 1);
    }

    private boolean sameField(Field a, Field b) {
        return a.getFile() == b.getFile() && a.getRank() == b.getRank();
    }
}
