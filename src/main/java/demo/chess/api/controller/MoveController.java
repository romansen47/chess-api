package demo.chess.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.BoardDto;
import demo.chess.api.dto.MoveRequestDto;
import demo.chess.api.dto.MoveResultDto;
import demo.chess.api.dto.PossibleMovesResponse;
import demo.chess.api.service.MoveApplicationService;
import demo.chess.api.service.MoveApplicationService.MoveOutcome;

/** REST boundary for board display, legal-move queries and human moves. */
@RestController
@RequestMapping("/api")
public class MoveController {

    private final MoveApplicationService moveApplicationService;

    public MoveController(MoveApplicationService moveApplicationService) {
        this.moveApplicationService = moveApplicationService;
    }

    @GetMapping("/possible-moves")
    public ResponseEntity<PossibleMovesResponse> getPossibleMoves(@RequestParam("from") String from) {
        return ResponseEntity.ok(moveApplicationService.getPossibleMoves(from));
    }

    @GetMapping("/board")
    public ResponseEntity<BoardDto> getBoard() {
        return ResponseEntity.ok(moveApplicationService.getBoard());
    }

    @PostMapping("/move")
    public ResponseEntity<MoveResultDto> makeMove(@RequestBody MoveRequestDto request) {
        MoveOutcome outcome = moveApplicationService.makeMove(request);
        return switch (outcome.type()) {
            case SUCCESS -> ResponseEntity.ok(outcome.result());
            case INVALID_REQUEST -> ResponseEntity.badRequest().body(outcome.result());
            case INTERNAL_ERROR -> ResponseEntity.internalServerError().body(outcome.result());
        };
    }
}
