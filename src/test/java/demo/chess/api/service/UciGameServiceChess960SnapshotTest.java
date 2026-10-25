package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

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
    @Test
    void importsChess960MovetextAndAnnotationsWithoutFallingBackToPosition518()
            throws Exception {
        GameService gameService = new GameService();
        UciGameService service = new UciGameService(gameService, null);
        String fen = "rqnbknbr/pppppppp/8/8/8/8/PPPPPPPP/RQNBKNBR w HAha - 0 1";
        ChessStartingPosition position = ChessStartingPosition.fromInitialFen(fen);
        String pgn = """
                [Event "Chess960 import regression"]
                [Variant "Chess960"]
                [SetUp "1"]
                [FEN "rqnbknbr/pppppppp/8/8/8/8/PPPPPPPP/RQNBKNBR w HAha - 0 1"]
                [Result "*"]

                1. h3 c6 2. Bh2 {[%eval 0.20]} Bc7 *
                """;

        UciGameDto imported = service.importGame(pgn, 43L);

        assertEquals(position.getId(), imported.getStartingPositionId());
        assertEquals(4, imported.getTotalPlies());
        assertEquals("g1h2", imported.getMoves().get(2).getUci());
        assertEquals("0.20", imported.getAnnotations().get(0).evaluation());
    }


    @Test
    void editsImportedMetadataWhileProtectingChess960Identity() throws Exception {
        GameService gameService = new GameService();
        UciGameService service = new UciGameService(gameService, null);
        ChessStartingPosition position = ChessStartingPosition.of(0);
        String pgn = "[Event \"Original\"]\n"
                + "[Site \"Old Site\"]\n"
                + "[Variant \"Chess960\"]\n"
                + "[SetUp \"1\"]\n"
                + "[FEN \"" + position.initialFen() + "\"]\n"
                + "[White \"Alice\"]\n"
                + "[Black \"Bob\"]\n"
                + "[Result \"*\"]\n\n*\n";

        UciGameDto imported = service.importGame(pgn, 42L);
        Map<String, String> edited = new LinkedHashMap<>(imported.getPgnTags());
        edited.put("Event", "Edited");
        edited.put("Site", "Stuttgart");
        edited.put("White", "Carol");
        edited.put("Black", "Dave");

        UciGameService.PreparedMetadataUpdate prepared =
                service.prepareMetadataUpdate(edited);
        service.applyMetadataUpdate(prepared);

        UciGameDto snapshot = service.getCurrentGameSnapshot().getGame();
        assertEquals("Edited", snapshot.getPgnTags().get("Event"));
        assertEquals("Stuttgart", snapshot.getPgnTags().get("Site"));
        assertEquals("Carol", snapshot.getWhitePlayerName());
        assertEquals("Dave", snapshot.getBlackPlayerName());
        assertEquals(position.initialFen(), snapshot.getPgnTags().get("FEN"));
        assertTrue(prepared.pgn().contains("[Event \"Edited\"]"));

        Map<String, String> illegal = new LinkedHashMap<>(edited);
        illegal.put("FEN", ChessStartingPosition.STANDARD.initialFen());
        assertThrows(
                IllegalArgumentException.class,
                () -> service.prepareMetadataUpdate(illegal));
    }
}
