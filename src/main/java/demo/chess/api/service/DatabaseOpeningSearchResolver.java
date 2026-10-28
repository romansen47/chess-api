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
     * Resolves either one exact line or all exact lines assigned to a variation.
     *
     * @param variationName complete stored variation name, optional
     * @param lineId exact opening line id, optional
     * @return exact move-order search prefixes
     */
    public List<OpeningLineSearch> resolve(String variationName, String lineId) {
        String variation = normalize(variationName);
        String requestedLineId = normalize(lineId);

        if (requestedLineId != null) {
            OpeningDto line = openingLibraryService.getOpening(requestedLineId);
            if (variation != null && !variation.equals(line.getName())) {
                throw new IllegalArgumentException(
                        "Selected opening line does not belong to variation: " + variation);
            }
            return List.of(toSearchLine(line));
        }

        if (variation == null) {
            return List.of();
        }

        List<OpeningDto> lines = openingLibraryService.getOpeningLinesForVariation(variation);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Unknown opening variation: " + variation);
        }
        return lines.stream().map(this::toSearchLine).toList();
    }

    private OpeningLineSearch toSearchLine(OpeningDto opening) {
        return new OpeningLineSearch(opening.getStartingPositionId(), opening.getMoves());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
