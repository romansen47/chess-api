package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.ObjectMapper;

import demo.chess.api.dto.OpeningCatalogImportResultDto;

/** Integration coverage for the pinned bundled Lichess opening catalog. */
class BundledOpeningCatalogServiceTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearStoreProperty() {
        System.clearProperty("chess.opening.library.file");
    }

    @Test
    void importsCompletePinnedLichessCatalogIdempotently() throws Exception {
        System.setProperty(
                "chess.opening.library.file",
                tempDir.resolve("openings.json").toString());
        OpeningLibraryService library = new OpeningLibraryService(new ObjectMapper());
        BundledOpeningCatalogService catalog = new BundledOpeningCatalogService(library);

        BundledOpeningCatalogInitializer initializer =
                new BundledOpeningCatalogInitializer(catalog);
        initializer.afterPropertiesSet();

        assertEquals(
                BundledOpeningCatalogService.LICHESS_CATALOG_ENTRIES,
                library.getOpenings().size());
        assertTrue(library.getOpenings().stream().anyMatch(opening ->
                "Sicilian Defense: Najdorf Variation, English Attack".equals(opening.getName())));

        OpeningCatalogImportResultDto second = catalog.ensureCatalogInstalled();

        assertEquals(0, second.importedCount());
        assertEquals(
                BundledOpeningCatalogService.LICHESS_CATALOG_ENTRIES,
                library.getOpenings().size());
    }
}
