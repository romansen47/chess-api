package demo.chess.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.ChessDatabaseDtos;
import demo.chess.api.dto.UciGameDto;
import demo.chess.database.ChessDatabaseStatus;
import demo.chess.database.GameSearch;
import demo.chess.database.ImportResult;
import demo.chess.database.PositionMoveStatistics;
import demo.chess.database.PositionStatistics;
import demo.chess.database.SqliteChessDatabase;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.DummyGame;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;
import demo.chess.notation.PgnNotation;

/**
 * Application-level bridge between the REST API and the embedded chess database.
 */
@Service
public class ChessDatabaseService {

    private final UciGameService uciGameService;
    private final Path databasePath;
    private final GameLoader gameLoader = new GameLoader();

    private volatile SqliteChessDatabase database;

    /**
     * Creates a new database service.
     *
     * @param uciGameService game import and analysis service
     */
    public ChessDatabaseService(UciGameService uciGameService) {
        this.uciGameService = uciGameService;
        this.databasePath = SqliteChessDatabase.defaultPath();
    }

    /**
     * Returns the database status without making database availability a startup requirement.
     *
     * @return status payload
     */
    public ChessDatabaseDtos.Status getStatus() {
        try {
            ChessDatabaseStatus status = database().getStatus();
            return new ChessDatabaseDtos.Status(
                    true,
                    status.path(),
                    status.name(),
                    status.schemaVersion(),
                    status.gameCount(),
                    status.sizeBytes(),
                    null);
        } catch (Exception e) {
            return new ChessDatabaseDtos.Status(
                    false,
                    databasePath.toString(),
                    "Chess Database",
                    null,
                    0L,
                    0L,
                    e.getMessage());
        }
    }

    /**
     * Imports a PGN stream into the local database.
     *
     * @param inputStream PGN stream
     * @return import summary
     */
    public ChessDatabaseDtos.ImportResult importPgn(InputStream inputStream)
            throws SQLException, IOException {
        ImportResult result = database().importPgn(inputStream);
        return new ChessDatabaseDtos.ImportResult(
                result.importedGames(),
                result.skippedGames(),
                result.totalPlies(),
                result.elapsedMillis());
    }

    /**
     * Searches stored games.
     *
     * @param request search request
     * @return matching game rows
     */
    public List<ChessDatabaseDtos.GameSummary> search(ChessDatabaseDtos.SearchRequest request)
            throws SQLException, IOException {
        ChessDatabaseDtos.SearchRequest safeRequest = request == null
                ? new ChessDatabaseDtos.SearchRequest(
                        null, null, null, null, null, null, null, 200)
                : request;

        GameSearch search = new GameSearch(
                safeRequest.white(),
                safeRequest.black(),
                safeRequest.player(),
                safeRequest.fromYear(),
                safeRequest.toYear(),
                safeRequest.result(),
                safeRequest.minElo(),
                safeRequest.limit() == null ? 200 : safeRequest.limit());

        return database().findGames(search).stream()
                .map(game -> new ChessDatabaseDtos.GameSummary(
                        game.id(),
                        game.date(),
                        game.white(),
                        game.black(),
                        game.whiteElo(),
                        game.blackElo(),
                        game.result(),
                        game.event(),
                        game.eco(),
                        game.plyCount()))
                .toList();
    }

    /**
     * Loads a stored game through the existing PGN analysis import path.
     *
     * @param gameId database game identifier
     * @return imported game payload
     */
    public UciGameDto loadGame(long gameId)
            throws SQLException, IOException, NoMoveFoundException {
        String pgn = database().getGameAsPgn(gameId);
        return uciGameService.importGame(pgn);
    }

    /**
     * Returns database continuations for the current analysis position.
     *
     * @param ply selected ply
     * @return position statistics
     */
    public ChessDatabaseDtos.PositionResult getPositionStatistics(int ply)
            throws SQLException, IOException, NoMoveFoundException {
        List<Move> analysisMoves = uciGameService.getAnalysisMoveListSnapshot();
        List<String> uciMoves = analysisMoves.stream()
                .map(Move::toString)
                .toList();

        int safePly = Math.max(0, Math.min(ply, uciMoves.size()));
        PositionStatistics statistics = database().findPosition(uciMoves, safePly);

        DummyGame notationGame = Simulation.createDummySimulation();
        if (safePly > 0) {
            gameLoader.loadGame(uciMoves.subList(0, safePly), notationGame);
        }

        List<ChessDatabaseDtos.PositionMove> moves = new ArrayList<>();
        for (PositionMoveStatistics moveStatistics : statistics.moves()) {
            String san = toSan(notationGame, moveStatistics.move());
            moves.add(new ChessDatabaseDtos.PositionMove(
                    moveStatistics.move(),
                    san,
                    moveStatistics.games(),
                    moveStatistics.whiteWins(),
                    moveStatistics.draws(),
                    moveStatistics.blackWins()));
        }

        long games = moves.stream()
                .mapToLong(ChessDatabaseDtos.PositionMove::games)
                .sum();

        return new ChessDatabaseDtos.PositionResult(safePly, games, moves);
    }

    /**
     * Converts an indexed UCI continuation to SAN in the selected position.
     *
     * @param game selected analysis position
     * @param uciMove UCI continuation
     * @return SAN notation or the original UCI move when no legal match is found
     */
    private String toSan(DummyGame game, String uciMove) throws NoMoveFoundException, IOException {
        for (Move move : game.getPlayer().getValidMoves(game)) {
            if (move.toString().equalsIgnoreCase(uciMove)) {
                return PgnNotation.toDisplayNotation(game, move);
            }
        }
        return uciMove;
    }

    /**
     * Lazily creates the embedded database so database file failures do not prevent app startup.
     *
     * @return initialized database
     */
    private SqliteChessDatabase database() throws SQLException, IOException {
        SqliteChessDatabase current = database;
        if (current != null) {
            return current;
        }

        synchronized (this) {
            if (database == null) {
                database = new SqliteChessDatabase(databasePath);
            }
            return database;
        }
    }
}
