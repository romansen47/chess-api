package demo.chess.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.ChessDatabaseDtos;
import demo.chess.api.dto.UciGameDto;
import demo.chess.database.ChessDatabaseStatus;
import demo.chess.database.GameSearch;
import demo.chess.database.ImportCancelledException;
import demo.chess.database.ImportProgress;
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
import jakarta.annotation.PreDestroy;

/**
 * Application-level bridge between the REST API and the embedded chess database.
 */
@Service
public class ChessDatabaseService {

    private final UciGameService uciGameService;
    private final Path databasePath;
    private final GameLoader gameLoader = new GameLoader();
    private final ExecutorService importExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "chess-database-import");
        thread.setDaemon(true);
        return thread;
    });
    private final ConcurrentMap<String, ImportJobState> importJobs = new ConcurrentHashMap<>();
    private final AtomicReference<String> activeImportId = new AtomicReference<>();

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
     * Copies an uploaded PGN to a temporary source and starts an asynchronous import job.
     *
     * @param fileName original file name
     * @param inputStream uploaded PGN stream
     * @return initial job status
     */
    public ChessDatabaseDtos.ImportJob startImport(String fileName, InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }

        String importId = UUID.randomUUID().toString();
        if (!activeImportId.compareAndSet(null, importId)) {
            throw new IllegalStateException("Another chess database import is already running.");
        }

        Path temporaryFile = Files.createTempFile("chess-database-import-", ".pgn");
        try {
            Files.copy(inputStream, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            long totalBytes = Files.size(temporaryFile);
            ImportJobState job = new ImportJobState(
                    importId,
                    fileName == null || fileName.isBlank() ? "database.pgn" : fileName,
                    totalBytes);
            importJobs.put(importId, job);
            importExecutor.submit(() -> runImport(job, temporaryFile));
            return job.snapshot();
        } catch (IOException | RuntimeException e) {
            activeImportId.compareAndSet(importId, null);
            Files.deleteIfExists(temporaryFile);
            throw e;
        }
    }

    /**
     * Returns the current state of an import job.
     *
     * @param importId job identifier
     * @return current job status
     */
    public ChessDatabaseDtos.ImportJob getImportJob(String importId) {
        return requireImportJob(importId).snapshot();
    }

    /**
     * Requests cancellation of a running import job.
     *
     * @param importId job identifier
     * @return current job status
     */
    public ChessDatabaseDtos.ImportJob cancelImport(String importId) {
        ImportJobState job = requireImportJob(importId);
        job.requestCancellation();
        return job.snapshot();
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
     * Stops the worker during normal application shutdown.
     */
    @PreDestroy
    public void shutdown() {
        String activeId = activeImportId.get();
        if (activeId != null) {
            ImportJobState job = importJobs.get(activeId);
            if (job != null) {
                job.requestCancellation();
            }
        }
        importExecutor.shutdownNow();
    }

    /**
     * Executes one import job and keeps its state observable by the REST API.
     *
     * @param job job state
     * @param temporaryFile temporary PGN source
     */
    private void runImport(ImportJobState job, Path temporaryFile) {
        try (InputStream inputStream = Files.newInputStream(temporaryFile)) {
            ImportResult result = database().importPgn(
                    job.id,
                    inputStream,
                    job.totalBytes,
                    job::updateProgress,
                    () -> job.cancellationRequested || Thread.currentThread().isInterrupted());
            job.complete(result);
        } catch (ImportCancelledException e) {
            job.cancelled();
        } catch (Exception e) {
            job.failed(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException ignored) {
                // Temporary file cleanup must not alter the final import state.
            }
            activeImportId.compareAndSet(job.id, null);
        }
    }

    /**
     * Resolves an import job or reports an unknown identifier.
     *
     * @param importId job identifier
     * @return job state
     */
    private ImportJobState requireImportJob(String importId) {
        ImportJobState job = importJobs.get(importId);
        if (job == null) {
            throw new NoSuchElementException("Chess database import job not found: " + importId);
        }
        return job;
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

    /**
     * Mutable thread-safe-enough state for one single-worker import job.
     */
    private static final class ImportJobState {

        private final String id;
        private final String fileName;
        private final long totalBytes;

        private volatile String status = "RUNNING";
        private volatile long bytesRead;
        private volatile long processedGames;
        private volatile long importedGames;
        private volatile long skippedGames;
        private volatile long totalPlies;
        private volatile long elapsedMillis;
        private volatile String message;
        private volatile boolean cancellationRequested;

        /**
         * Creates one running import job.
         */
        private ImportJobState(String id, String fileName, long totalBytes) {
            this.id = id;
            this.fileName = fileName;
            this.totalBytes = totalBytes;
        }

        /**
         * Applies a running database progress snapshot.
         */
        private void updateProgress(ImportProgress progress) {
            bytesRead = progress.bytesRead();
            processedGames = progress.processedGames();
            importedGames = progress.importedGames();
            skippedGames = progress.skippedGames();
            totalPlies = progress.totalPlies();
            elapsedMillis = progress.elapsedMillis();
        }

        /**
         * Requests cancellation when the job is still running.
         */
        private void requestCancellation() {
            if ("RUNNING".equals(status)) {
                cancellationRequested = true;
                message = "Cancellation requested…";
            }
        }

        /**
         * Marks the job complete after the staged data was atomically published.
         */
        private void complete(ImportResult result) {
            importedGames = result.importedGames();
            skippedGames = result.skippedGames();
            totalPlies = result.totalPlies();
            elapsedMillis = result.elapsedMillis();
            bytesRead = totalBytes;
            message = "Import complete.";
            status = "COMPLETE";
        }

        /**
         * Marks the job cancelled after staged data was removed.
         */
        private void cancelled() {
            message = "Import cancelled. No games from this import were added to the active database.";
            status = "CANCELLED";
        }

        /**
         * Marks the job failed after staged data was removed.
         */
        private void failed(String failureMessage) {
            message = "Import failed: " + failureMessage;
            status = "FAILED";
        }

        /**
         * Creates an immutable REST snapshot.
         */
        private ChessDatabaseDtos.ImportJob snapshot() {
            return new ChessDatabaseDtos.ImportJob(
                    id,
                    fileName,
                    status,
                    totalBytes,
                    bytesRead,
                    processedGames,
                    importedGames,
                    skippedGames,
                    totalPlies,
                    elapsedMillis,
                    message);
        }
    }
}
