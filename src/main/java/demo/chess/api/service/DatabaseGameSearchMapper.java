package demo.chess.api.service;

import java.util.List;

import org.springframework.stereotype.Component;

import demo.chess.api.dto.ChessDatabaseDtos;
import demo.chess.database.GameSearch;
import demo.chess.database.OpeningLineSearch;

/** Maps REST database-search criteria to persistence-layer search criteria. */
@Component
public class DatabaseGameSearchMapper {

    private final DatabaseOpeningSearchResolver openingSearchResolver;

    public DatabaseGameSearchMapper(DatabaseOpeningSearchResolver openingSearchResolver) {
        this.openingSearchResolver = openingSearchResolver;
    }

    public GameSearch map(ChessDatabaseDtos.SearchRequest request) {
        ChessDatabaseDtos.SearchRequest safeRequest = request == null
                ? new ChessDatabaseDtos.SearchRequest(
                        null, null, null, null, null, null, null, null, 200)
                : request;
        List<OpeningLineSearch> openingLines = openingSearchResolver.resolve(
                safeRequest.openingFamily(),
                safeRequest.openingVariation(),
                safeRequest.openingLineId());
        return new GameSearch(
                safeRequest.white(),
                safeRequest.black(),
                safeRequest.player(),
                safeRequest.player2(),
                safeRequest.fromYear(),
                safeRequest.toYear(),
                safeRequest.result(),
                safeRequest.minElo(),
                safeRequest.eco(),
                openingLines,
                safeRequest.limit() == null ? 200 : safeRequest.limit());
    }
}
