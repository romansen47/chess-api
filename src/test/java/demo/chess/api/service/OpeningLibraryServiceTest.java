package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.ObjectMapper;

import demo.chess.api.dto.OpeningDto;

class OpeningLibraryServiceTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearStoreProperty() {
        System.clearProperty("chess.opening.library.file");
    }

    @Test
    void importsMultiplePgnGamesAndPersistsReusableOpenings() throws Exception {
        Path store = tempDir.resolve("openings.json");
        System.setProperty("chess.opening.library.file", store.toString());
        OpeningLibraryService service = new OpeningLibraryService(new ObjectMapper());

        String pgn = """
                [Opening "Sicilian"]
                [ECO "B20"]
                [Result "*"]

                1. e4 c5 *

                [Variation "French Advance"]
                [Result "*"]

                1. e4 e6 2. d4 d5 3. e5 *
                """;

        List<OpeningDto> imported = service.importPgn(pgn, "suite.pgn");

        assertEquals(2, imported.size());
        assertEquals("Sicilian", imported.get(0).getName());
        assertEquals("B20", imported.get(0).getEco());
        assertEquals(List.of("e2e4", "c7c5"), imported.get(0).getMoves());
        assertEquals(List.of("e4", "c5"), imported.get(0).getSanMoves());
        assertTrue(imported.get(0).isPlayable());
        assertEquals("French Advance", imported.get(1).getName());
        assertTrue(java.nio.file.Files.isRegularFile(store));

        OpeningLibraryService reloaded = new OpeningLibraryService(new ObjectMapper());
        assertEquals(2, reloaded.getOpenings().size());
    }

    @Test
    void groupsMultipleExactMoveOrdersUnderOneVariation() throws Exception {
        System.setProperty("chess.opening.library.file", tempDir.resolve("openings.json").toString());
        OpeningLibraryService service = new OpeningLibraryService(new ObjectMapper());

        String pgn = """
                [Opening "Grünfeld Defense: Russian Variation"]
                [ECO "D96"]
                [Result "*"]

                1. d4 Nf6 2. c4 g6 3. Nc3 d5 4. Nf3 Bg7 5. Qb3 *

                [Opening "Grünfeld Defense: Russian Variation"]
                [ECO "D97"]
                [Result "*"]

                1. Nf3 Nf6 2. c4 g6 3. Nc3 Bg7 4. d4 O-O 5. Qb3 *
                """;

        List<OpeningDto> imported = service.importPgn(pgn, "gruenfeld.pgn");

        assertEquals(2, imported.size());
        assertEquals("Grünfeld Defense", imported.get(0).getFamily());
        assertEquals("Russian Variation", imported.get(0).getVariation());
        assertEquals(2, service.getOpenings().stream()
                .filter(opening -> "Grünfeld Defense: Russian Variation".equals(opening.getName()))
                .count());
    }

    @Test
    void usesFileNameWhenPgnHasNoOpeningMetadata() throws Exception {
        System.setProperty("chess.opening.library.file", tempDir.resolve("openings.json").toString());
        OpeningLibraryService service = new OpeningLibraryService(new ObjectMapper());

        OpeningDto opening = service.importPgn(
                "[Result \"*\"]\n\n1. d4 d5 *",
                "quiet-lines.pgn").get(0);

        assertEquals("quiet-lines", opening.getName());
    }

    @Test
    void rejectsBrokenMultiGameImportAtomically() {
        System.setProperty("chess.opening.library.file", tempDir.resolve("openings.json").toString());
        OpeningLibraryService service = new OpeningLibraryService(new ObjectMapper());

        String pgn = """
                [Opening "Valid"]
                [Result "*"]

                1. e4 e5 *

                [Opening "Broken"]
                [Result "*"]

                1. e4 e5 2. Ke2 Ke7 3. Ke3 *
                """;

        assertThrows(Exception.class, () -> service.importPgn(pgn + " 3... Ke6 4. Kf3 Kf6 5. Kg3 Kg6 6. Kh3 Kh6 7. Qh5+ *", "mixed.pgn"));
        assertTrue(service.getOpenings().isEmpty());
    }


    @Test
    void deduplicatesIdenticalOpeningLinesWithinOneImport() throws Exception {
        System.setProperty("chess.opening.library.file", tempDir.resolve("openings.json").toString());
        OpeningLibraryService service = new OpeningLibraryService(new ObjectMapper());

        String pgn = """
                [Opening "Italian Game"]
                [ECO "C50"]
                [Result "1-0"]

                1. e4 e5 2. Nf3 Nc6 1-0

                [Opening "Italian Game"]
                [ECO "C50"]
                [Result "0-1"]

                1. e4 e5 2. Nf3 Nc6 0-1

                [Opening "Italian Game"]
                [ECO "C50"]
                [Result "1/2-1/2"]

                1. e4 e5 2. Nf3 Nc6 1/2-1/2
                """;

        List<OpeningDto> imported = service.importPgn(pgn, "duplicates.pgn");

        assertEquals(1, imported.size());
        assertEquals(1, service.getOpenings().size());
        assertEquals("Italian Game", service.getOpenings().get(0).getName());
    }

    @Test
    void removesLegacyDuplicateLinesWhenLoadingTheStore() throws Exception {
        Path store = tempDir.resolve("openings.json");
        System.setProperty("chess.opening.library.file", store.toString());
        java.nio.file.Files.writeString(store, """
                {
                  "version" : 3,
                  "openings" : [ {
                    "id" : "one",
                    "name" : "Italian Game",
                    "eco" : "C50",
                    "startingPositionId" : 518,
                    "moves" : [ "e2e4", "e7e5", "g1f3", "b8c6" ]
                  }, {
                    "id" : "two",
                    "name" : "Italian Game duplicate",
                    "eco" : "C50",
                    "startingPositionId" : 518,
                    "moves" : [ "e2e4", "e7e5", "g1f3", "b8c6" ]
                  } ]
                }
                """);

        OpeningLibraryService service = new OpeningLibraryService(new ObjectMapper());

        assertEquals(1, service.getOpenings().size());
        OpeningLibraryService reloaded = new OpeningLibraryService(new ObjectMapper());
        assertEquals(1, reloaded.getOpenings().size());
    }

    @Test
    void marksCheckmatedLineAsNotPlayable() throws Exception {
        System.setProperty("chess.opening.library.file", tempDir.resolve("openings.json").toString());
        OpeningLibraryService service = new OpeningLibraryService(new ObjectMapper());

        OpeningDto opening = service.importPgn(
                "[Opening \"Fools mate\"]\n[Result \"*\"]\n\n1. f3 e5 2. g4 Qh4# *",
                "mate.pgn").get(0);

        assertFalse(opening.isPlayable());
    }
}
