package demo.chess.api.dto;

/** Result of importing a bundled opening catalog. */
public record OpeningCatalogImportResultDto(
        int importedCount,
        int catalogEntries,
        String sourceRevision) {
}
