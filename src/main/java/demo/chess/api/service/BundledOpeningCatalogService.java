package demo.chess.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.OpeningCatalogImportResultDto;
import demo.chess.api.dto.OpeningDto;
import demo.chess.definitions.engines.impl.NoMoveFoundException;

/** Installs the pinned public-domain Lichess opening catalog bundled with the application. */
@Service
public class BundledOpeningCatalogService {

    public static final String LICHESS_SOURCE_REVISION =
            "e76b8b4ec5c8aeeba41dac6caf4a60b938f652db";
    public static final int LICHESS_CATALOG_ENTRIES = 3_865;

    private static final String LICHESS_RESOURCE = "/openings/lichess-openings.pgn";
    private static final String LICHESS_FILE_NAME =
            "lichess-openings-" + LICHESS_SOURCE_REVISION.substring(0, 12) + ".pgn";

    private final OpeningLibraryService openingLibraryService;

    public BundledOpeningCatalogService(OpeningLibraryService openingLibraryService) {
        this.openingLibraryService = openingLibraryService;
    }

    /**
     * Imports every bundled Lichess opening line while preserving its exact move order.
     *
     * @return import result including the pinned source revision
     */
    public OpeningCatalogImportResultDto ensureCatalogInstalled()
            throws IOException, NoMoveFoundException {
        String content = readCatalog();
        List<OpeningDto> imported = openingLibraryService.importPgn(content, LICHESS_FILE_NAME);
        return new OpeningCatalogImportResultDto(
                imported.size(),
                LICHESS_CATALOG_ENTRIES,
                LICHESS_SOURCE_REVISION);
    }

    private String readCatalog() throws IOException {
        try (InputStream input = BundledOpeningCatalogService.class
                .getResourceAsStream(LICHESS_RESOURCE)) {
            if (input == null) {
                throw new IOException("Bundled Lichess opening catalog is missing");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
