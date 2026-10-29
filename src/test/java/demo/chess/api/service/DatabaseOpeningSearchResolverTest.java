package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.ObjectMapper;

import demo.chess.api.dto.OpeningDto;
import demo.chess.database.OpeningLineSearch;

/** Tests variation and exact-line resolution for database search. */
class DatabaseOpeningSearchResolverTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearStoreProperty() {
        System.clearProperty("chess.opening.library.file");
    }

    @Test
    void resolvesVariationToAllExactMoveOrdersAndLineToOne() throws Exception {
        System.setProperty("chess.opening.library.file", tempDir.resolve("openings.json").toString());
        OpeningLibraryService library = new OpeningLibraryService(new ObjectMapper());
        List<OpeningDto> imported = library.importPgn("""
                [Opening "Grünfeld Defense: Russian Variation"]
                [Result "*"]

                1. d4 Nf6 2. c4 g6 *

                [Opening "Grünfeld Defense: Russian Variation"]
                [Result "*"]

                1. Nf3 Nf6 2. c4 g6 *

                [Opening "Grünfeld Defense: Exchange Variation"]
                [Result "*"]

                1. d4 Nf6 2. c4 g6 3. Nc3 d5 *
                """, "lines.pgn");
        DatabaseOpeningSearchResolver resolver = new DatabaseOpeningSearchResolver(library);

        List<OpeningLineSearch> family =
                resolver.resolve("Grünfeld Defense", null, null);
        assertEquals(3, family.size());

        List<OpeningLineSearch> variation =
                resolver.resolve("Grünfeld Defense: Russian Variation", null);
        assertEquals(2, variation.size());

        List<OpeningLineSearch> exact =
                resolver.resolve("Grünfeld Defense: Russian Variation", imported.get(0).getId());
        assertEquals(1, exact.size());
        assertEquals(imported.get(0).getMoves(), exact.get(0).moves());
    }

    @Test
    void rejectsLineFromAnotherVariation() throws Exception {
        System.setProperty("chess.opening.library.file", tempDir.resolve("openings.json").toString());
        OpeningLibraryService library = new OpeningLibraryService(new ObjectMapper());
        OpeningDto line = library.importPgn(
                "[Opening \"Italian Game\"]\n[Result \"*\"]\n\n1. e4 e5 *",
                "line.pgn").get(0);
        DatabaseOpeningSearchResolver resolver = new DatabaseOpeningSearchResolver(library);

        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve("Ruy Lopez", line.getId()));
    }
}
