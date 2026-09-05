package demo.chess.api.dto;

import java.util.List;

/**
 * REST payload types for the local chess database.
 */
public final class ChessDatabaseDtos {

    /**
     * Prevents instantiation.
     */
    private ChessDatabaseDtos() {
    }

    /**
     * Database status payload.
     *
     * @param available whether the database can be opened
     * @param path database file path
     * @param name database display name
     * @param schemaVersion schema version
     * @param gameCount stored game count
     * @param sizeBytes current database size
     * @param message optional status message
     */
    public record Status(
            boolean available,
            String path,
            String name,
            Integer schemaVersion,
            long gameCount,
            long sizeBytes,
            String message) {
    }

    /**
     * Completed import payload.
     *
     * @param importedGames successfully imported games
     * @param skippedGames skipped games
     * @param totalPlies imported half-moves
     * @param elapsedMillis elapsed import time
     */
    public record ImportResult(
            long importedGames,
            long skippedGames,
            long totalPlies,
            long elapsedMillis) {
    }

    /**
     * Observable background import job.
     *
     * @param id job identifier
     * @param fileName source file name
     * @param status RUNNING, COMPLETE, CANCELLED or FAILED
     * @param totalBytes total source size
     * @param bytesRead bytes consumed by the importer
     * @param processedGames number of PGN games processed
     * @param importedGames successfully staged or published games
     * @param skippedGames skipped games
     * @param totalPlies staged or published half-moves
     * @param elapsedMillis elapsed import time
     * @param message optional result or error message
     */
    public record ImportJob(
            String id,
            String fileName,
            String status,
            long totalBytes,
            long bytesRead,
            long processedGames,
            long importedGames,
            long skippedGames,
            long totalPlies,
            long elapsedMillis,
            String message) {
    }

    /**
     * Game search request payload.
     *
     * @param white white player fragment
     * @param black black player fragment
     * @param player either player fragment
     * @param fromYear minimum year
     * @param toYear maximum year
     * @param result PGN result
     * @param minElo minimum rating for both players
     * @param limit maximum result count
     */
    public record SearchRequest(
            String white,
            String black,
            String player,
            Integer fromYear,
            Integer toYear,
            String result,
            Integer minElo,
            Integer limit) {
    }

    /**
     * Search result row.
     *
     * @param id database identifier
     * @param date PGN date
     * @param white white player
     * @param black black player
     * @param whiteElo white rating
     * @param blackElo black rating
     * @param result result
     * @param event event
     * @param eco ECO code
     * @param plyCount number of half-moves
     */
    public record GameSummary(
            long id,
            String date,
            String white,
            String black,
            Integer whiteElo,
            Integer blackElo,
            String result,
            String event,
            String eco,
            int plyCount) {
    }

    /**
     * Position continuation row.
     *
     * @param uci UCI move
     * @param san SAN display notation
     * @param games occurrence count
     * @param whiteWins white wins
     * @param draws draws
     * @param blackWins black wins
     */
    public record PositionMove(
            String uci,
            String san,
            long games,
            long whiteWins,
            long draws,
            long blackWins) {
    }

    /**
     * Position statistics payload.
     *
     * @param ply selected ply
     * @param games total indexed continuations from the position
     * @param moves continuation statistics
     */
    public record PositionResult(
            int ply,
            long games,
            List<PositionMove> moves) {

        /**
         * Creates an immutable move list.
         */
        public PositionResult {
            moves = List.copyOf(moves);
        }
    }
}
