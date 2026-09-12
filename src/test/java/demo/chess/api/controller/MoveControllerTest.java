package demo.chess.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import demo.chess.api.dto.MoveRequestDto;
import demo.chess.api.dto.MoveResultDto;
import demo.chess.api.service.GameService;

class MoveControllerTest {

    private GameService gameService;

    @AfterEach
    void stopClocks() {
        if (gameService == null || gameService.getCurrentGame() == null) {
            return;
        }
        stopClock(gameService.getCurrentGame().getWhitePlayer().getChessClock());
        stopClock(gameService.getCurrentGame().getBlackPlayer().getChessClock());
    }

    @Test
    void successfulMoveResponsesExposeAuthoritativePly() {
        gameService = new GameService();
        MoveController controller = new MoveController(gameService);

        ResponseEntity<MoveResultDto> whiteResponse =
                controller.makeMove(new MoveRequestDto("e2", "e4", null));
        ResponseEntity<MoveResultDto> blackResponse =
                controller.makeMove(new MoveRequestDto("e7", "e5", null));

        assertNotNull(whiteResponse.getBody());
        assertNotNull(blackResponse.getBody());
        assertEquals(1, whiteResponse.getBody().getPly());
        assertEquals(2, blackResponse.getBody().getPly());
    }

    private void stopClock(demo.chess.definitions.clocks.impl.ChessClock clock) {
        if (clock != null && clock.isStarted() && !clock.isStopped()) {
            clock.stop();
        }
    }
}
