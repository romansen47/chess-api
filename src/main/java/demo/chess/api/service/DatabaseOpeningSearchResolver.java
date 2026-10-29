package demo.chess.api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.OpeningDto;
import demo.chess.database.OpeningLineSearch;

/** Resolves opening-library selections into exact database move-order prefixes. */
@Service
public class DatabaseOpeningSearchResolver {

    private final OpeningLibraryService openingLibraryService;

    public DatabaseOpeningSearchResolver(OpeningLibraryService openingLibraryService) {
        this.openingLibraryService = openingLibraryService;
    }

    /**
     * Resolves one exact line, one variation, or every line in an opening family.
     *
     * @param familyName complete stored opening-family name, optional
     * @param variationName complete stored variation name, optional
     * @param lineId exact opening line id, optional
     * @return exact move-order search prefixes
     */
    public List<OpeningLineSearch> resolve(
            String familyName,
            String variationName,
            String lineId) {
        String family = normalize(familyName);
        String variation = normalize(variationName);
        String requestedLineId = normalize(lineId);

        if (requestedLineId != null) {
            OpeningDto line = openingLibraryService.getOpening(requestedLineId);
            if (family != null && !family.equals(familyOf(line))) {
                throw new IllegalArgumentException(
                        "Selected opening line does not belong to family: " + family);
            }
            if (variation != null && !variation.equals(line.getName())) {
                throw new IllegalArgumentException(
                        "Selected opening line does not belong to variation: " + variation);
            }
            return List.of(toSearchLine(line));
        }

        if (variation == null && family == null) {
            return List.of();
        }

        List<OpeningDto> lines = openingLibraryService.getOpenings().stream()
                .filter(opening -> family == null || family.equals(familyOf(opening)))
                .filter(opening -> variation == null || variation.equals(opening.getName()))
                .toList();
        if (lines.isEmpty()) {
            String selection = variation != null
                    ? "variation: " + variation
                    : "family: " + family;
            throw new IllegalArgumentException("Unknown opening " + selection);
        }
        return lines.stream().map(this::toSearchLine).toList();
    }

    /** Backward-compatible variation/exact-line resolution. */
    public List<OpeningLineSearch> resolve(String variationName, String lineId) {
        return resolve(null, variationName, lineId);
    }

    private OpeningLineSearch toSearchLine(OpeningDto opening) {
        return new OpeningLineSearch(opening.getStartingPositionId(), opening.getMoves());
    }

    private String familyOf(OpeningDto opening) {
        String family = normalize(opening.getFamily());
        return family != null ? family : normalize(opening.getName());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
