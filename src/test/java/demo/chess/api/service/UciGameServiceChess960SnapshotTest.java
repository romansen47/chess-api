package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import demo.chess.api.dto.GameSnapshotDto;
import demo.chess.api.dto.UciGameDto;
import demo.chess.definitions.ChessStartingPosition;

class UciGameServiceChess960SnapshotTest {

    @Test
    void importedChess960ContextSurvivesSnapshotRoundTrip() throws Exception {
        GameService gameService = new GameService();
        UciGameService service = new UciGameService(gameService, null);
        ChessStartingPosition position = ChessStartingPosition.of(0);
        String pgn = "[Variant \"Chess960\"]\n"
                + "[SetUp \"1\"]\n"
                + "[FEN \"" + position.initialFen() + "\"]\n"
                + "[White \"Alice\"]\n"
                + "[Black \"Bob\"]\n\n*\n";

        UciGameDto imported = service.importGame(pgn, 42L);
        GameSnapshotDto snapshot = service.getCurrentGameSnapshot();

        assertEquals(0, imported.getStartingPositionId());
        assertEquals(position.initialFen(), imported.getInitialFen());
        assertTrue(snapshot.isImportedAnalysisGame());
        assertEquals(0, snapshot.getGame().getStartingPositionId());
        assertEquals(position.initialFen(), snapshot.getGame().getInitialFen());
        assertEquals(42L, snapshot.getGame().getDatabaseGameId());
        assertEquals("Alice", snapshot.getGame().getWhitePlayerName());
        assertEquals("Bob", snapshot.getGame().getBlackPlayerName());
    }
}
