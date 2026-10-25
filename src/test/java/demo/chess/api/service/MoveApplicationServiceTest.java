package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import demo.chess.api.dto.MoveRequestDto;
import demo.chess.api.dto.PossibleMovesResponse;
import demo.chess.api.service.MoveApplicationService.MoveOutcomeType;

class MoveApplicationServiceTest {

    private GameService gameService;

    /**
     * Verifies that the application boundary exposes canonical legal-move data
     * while leaving move legality in the chess core.
     */
    @Test
    void possibleMovesExposeCanonicalCoreMoveDescriptors() {
        gameService = new GameService();
        MoveApplicationService service = new MoveApplicationService(gameService);

        PossibleMovesResponse response = service.getPossibleMoves("e2");

        assertEquals(Set.of("e3", "e4"), Set.copyOf(response.getTargets()));
        assertEquals(
                Set.of("e2e3", "e2e4"),
                response.getMoves().stream().map(move -> move.uci()).collect(Collectors.toSet()));
    }

    /** Verifies that malformed move requests do not mutate the current game. */
    @Test
    void invalidRequestDoesNotAdvanceTheGame() {
        gameService = new GameService();
        MoveApplicationService service = new MoveApplicationService(gameService);

        var outcome = service.makeMove(new MoveRequestDto(null, "e4", null));

        assertEquals(MoveOutcomeType.INVALID_REQUEST, outcome.type());
        assertEquals(0, gameService.getCurrentGame().getMoveList().size());
        assertTrue(outcome.result().getMessage().contains("from/to"));
    }

    /** Stops clocks created by the real application service fixture. */
    @AfterEach
    void stopClocks() {
        if (gameService == null || gameService.getCurrentGame() == null) return;
        var white = gameService.getCurrentGame().getWhitePlayer().getChessClock();
        var black = gameService.getCurrentGame().getBlackPlayer().getChessClock();
        if (white != null) white.stop();
        if (black != null) black.stop();
    }
}
