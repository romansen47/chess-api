package demo.chess.api.controller;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.MoveRequestDto;
import demo.chess.api.dto.MoveResultDto;
import demo.chess.api.dto.BoardDto;
import demo.chess.api.dto.PossibleMovesResponse;
import demo.chess.api.service.GameService;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.game.Game;

@RestController
@RequestMapping("/api")
public class MoveController {

    private final GameService gameService;

    public MoveController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Liefert alle legalen Ziele für ein gegebenes Ausgangsfeld (z.B. "e2").
     */
    @GetMapping("/possible-moves")
    public ResponseEntity<PossibleMovesResponse> getPossibleMoves(
            @RequestParam("from") String from) throws NoMoveFoundException, IOException {

        Game game = gameService.getCurrentGame();
        Board board = game.getChessBoard();

        Field fromField = mapSquareToField(board, from);
        if (fromField == null || fromField.getPiece() == null) {
            // kein Feld / keine Figur -> leere Liste zurück
            return ResponseEntity.ok(new PossibleMovesResponse(from, List.of()));
        }

        List<String> targets = game.getPlayer()
                .getValidMoves(game).stream()
                .filter(m -> sameField(m.getSource(), fromField))
                .map(m -> m.getTarget().getName()) // z.B. "e4"
                .collect(Collectors.toList());

        return ResponseEntity.ok(new PossibleMovesResponse(from, targets));
    }

    @GetMapping("/board")
    public ResponseEntity<BoardDto> getBoard() {
    	BoardDto board = gameService.getBoardView();
    	return ResponseEntity.ok(board);
    }

    /**
     * Führt einen Zug aus ("from" -> "to").
     * Nach erfolgreichem Aufruf ist der Game-Zustand im Backend fortgeschrieben
     * und der nächste Spieler ist am Zug.
     */
    @PostMapping("/move")
    public ResponseEntity<MoveResultDto> makeMove(@RequestBody MoveRequestDto request) {

        String from = request.getFrom();
        String to = request.getTo();
        String promotion = request.getPromotion();

        if (from == null || to == null) {
            MoveResultDto error = new MoveResultDto(
                    false,
                    "from/to must not be null",
                    from,
                    to,
                    null,
                    null);
            return ResponseEntity.badRequest().body(error);
        }

        try {
            // Zug im Backend ausführen
            gameService.applyMove(from, to, promotion);

            Game game = gameService.getCurrentGame();

            // Letzten SAN-Zug aus der Liste holen (wird in ChessGame.apply() gepflegt)
            String san = null;
            List<String> sanMoves = game.getSanMoveList();
            if (sanMoves != null && !sanMoves.isEmpty()) {
                san = sanMoves.get(sanMoves.size() - 1);
            }

            String sideToMove = game.getPlayer() != null && game.getPlayer().getColor() != null
                    ? game.getPlayer().getColor().name().toLowerCase(Locale.ROOT)
                    : null;
            String position = gameService.getCurrentPositionString();
            String gameState = game.getState() != null ? game.getState().name() : null;

            MoveResultDto result = new MoveResultDto(
                    true,
                    null,
                    from,
                    to,
                    san,
                    sideToMove,
                    position,
                    gameState);

            return ResponseEntity.ok(result);

        } catch (NoMoveFoundException e) {
            MoveResultDto error = new MoveResultDto(
                    false,
                    e.getMessage(),
                    from,
                    to,
                    null,
                    null);
            return ResponseEntity.badRequest().body(error);
        } catch (IOException e) {
            MoveResultDto error = new MoveResultDto(
                    false,
                    "I/O error while applying move",
                    from,
                    to,
                    null,
                    null);
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Im alten Code hast du DisplayedField-Liste durchsucht.
     * Hier mappen wir "e2" -> file/rank und holen direkt vom Board.
     */
    private Field mapSquareToField(Board board, String square) {
        if (square == null || square.length() != 2) {
            return null;
        }
        square = square.toLowerCase(Locale.ROOT);
        char fileChar = square.charAt(0); // 'a'..'h'
        char rankChar = square.charAt(1); // '1'..'8'

        if (fileChar < 'a' || fileChar > 'h') return null;
        if (rankChar < '1' || rankChar > '8') return null;

        int file = fileChar - 'a' + 1;      // a->1, b->2, ...
        int rank = rankChar - '1' + 1;      // '1'->1, ...

        return board.getField(file, rank);
    }

    private boolean sameField(Field a, Field b) {
        return a.getFile() == b.getFile() && a.getRank() == b.getRank();
    }
}
