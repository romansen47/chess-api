package demo.chess.api.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.ChessDatabaseDtos;
import demo.chess.api.dto.UciGameDto;
import demo.chess.database.ChessDatabaseStatus;
import demo.chess.database.ChessDatabase;
import demo.chess.database.GameSearch;
import demo.chess.database.ImportCancelledException;
import demo.chess.database.ImportProgress;
import demo.chess.database.ImportResult;
import demo.chess.database.PositionMoveStatistics;
import demo.chess.database.PositionStatistics;
import demo.chess.database.SqliteChessDatabase;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.DummyGame;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;
import demo.chess.notation.PgnNotation;
import demo.chess.notation.UciMoveCodec;
import jakarta.annotation.PreDestroy;

/** Application-level bridge between the REST API and the embedded chess database. */
@Service
public class ChessDatabaseService {

    private final UciGameService uciGameService;
    private final DatabaseGameSearchMapper gameSearchMapper;
    private final Path databasePath;
    private final GameLoader gameLoader;
    private final ChessAnalysisDiagnosticPgnSanitizer diagnosticPgnSanitizer;
    private final DatabaseFactory databaseFactory;
    private final ExecutorService importExecutor;
    private final ImportTemporaryFileStore temporaryFileStore;
    private final LongSupplier nanoTime;
    private final ConcurrentMap<String, ImportJobState> importJobs = new ConcurrentHashMap<>();
    private final AtomicReference<String> activeImportId = new AtomicReference<>();

    private volatile ChessDatabase database;

    public ChessDatabaseService(
            UciGameService uciGameService,
            DatabaseGameSearchMapper gameSearchMapper) {
        this(
                uciGameService,
                gameSearchMapper,
                SqliteChessDatabase.defaultPath(),
                SqliteChessDatabase::new,
                createImportExecutor(),
                new ImportTemporaryFileStore(),
                System::nanoTime,
                new GameLoader(),
                new ChessAnalysisDiagnosticPgnSanitizer());
    }

    ChessDatabaseService(
            UciGameService uciGameService,
            DatabaseGameSearchMapper gameSearchMapper,
            Path databasePath,
            DatabaseFactory databaseFactory,
            ExecutorService importExecutor,
            ImportTemporaryFileStore temporaryFileStore,
            LongSupplier nanoTime,
            GameLoader gameLoader,
            ChessAnalysisDiagnosticPgnSanitizer diagnosticPgnSanitizer) {
        this.uciGameService = uciGameService;
        this.gameSearchMapper = gameSearchMapper;
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath");
        this.databaseFactory = Objects.requireNonNull(databaseFactory, "databaseFactory");
        this.importExecutor = Objects.requireNonNull(importExecutor, "importExecutor");
        this.temporaryFileStore = Objects.requireNonNull(temporaryFileStore, "temporaryFileStore");
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
        this.gameLoader = Objects.requireNonNull(gameLoader, "gameLoader");
        this.diagnosticPgnSanitizer = Objects.requireNonNull(
                diagnosticPgnSanitizer,
                "diagnosticPgnSanitizer");
    }

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

    public ImportResult importSingleGame(String content) throws SQLException, IOException {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("PGN content must not be blank.");
        }
        String importId = UUID.randomUUID().toString();
        if (!activeImportId.compareAndSet(null, importId)) {
            throw new IllegalStateException("Another chess database import is already running.");
        }
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return database().importPgn(importId, inputStream, bytes.length, null, () -> false);
        } finally {
            activeImportId.compareAndSet(importId, null);
        }
    }

    public long importSingleGameAndResolveId(String content)
            throws SQLException, IOException, NoMoveFoundException {
        String storedPgn = diagnosticPgnSanitizer.sanitize(content);
        importSingleGame(storedPgn);
        return database().findGameId(storedPgn);
    }

    public void saveAnnotatedPgn(long gameId, String pgn) throws SQLException, IOException {
        database().saveAnnotatedPgn(gameId, pgn);
    }

    public ChessDatabaseDtos.ImportJob startImport(String fileName, InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }
        String importId = UUID.randomUUID().toString();
        if (!activeImportId.compareAndSet(null, importId)) {
            throw new IllegalStateException("Another chess database import is already running.");
        }
        Path temporaryFile = null;
        try {
            temporaryFile = temporaryFileStore.stage(inputStream);
            long totalBytes = temporaryFileStore.size(temporaryFile);
            ImportJobState job = new ImportJobState(
                    importId,
                    fileName == null || fileName.isBlank() ? "database.pgn" : fileName,
                    totalBytes,
                    nanoTime);
            importJobs.put(importId, job);
            Path stagedFile = temporaryFile;
            importExecutor.submit(() -> runImport(job, stagedFile));
            return job.snapshot();
        } catch (IOException | RuntimeException e) {
            activeImportId.compareAndSet(importId, null);
            if (temporaryFile != null) temporaryFileStore.deleteIfExists(temporaryFile);
            throw e;
        }
    }

    public ChessDatabaseDtos.ImportJob getImportJob(String importId) {
        return requireImportJob(importId).snapshot();
    }

    public ChessDatabaseDtos.ImportJob cancelImport(String importId) {
        ImportJobState job = requireImportJob(importId);
        job.requestCancellation();
        return job.snapshot();
    }

    public List<ChessDatabaseDtos.GameSummary> search(ChessDatabaseDtos.SearchRequest request)
            throws SQLException, IOException {
        GameSearch search = gameSearchMapper.map(request);
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

    public UciGameDto loadGame(long gameId)
            throws SQLException, IOException, NoMoveFoundException {
        String pgn = database().getGameAsPgn(gameId);
        String sanitizedPgn = diagnosticPgnSanitizer.sanitize(pgn);
        if (!sanitizedPgn.equals(pgn)) {
            database().saveAnnotatedPgn(gameId, sanitizedPgn);
        }
        return uciGameService.importGame(sanitizedPgn, gameId);
    }

    /** Deletes one game and its contribution to the position statistics. */
    public void deleteGame(long gameId) throws SQLException, IOException {
        database().deleteGame(gameId);
        if (Objects.equals(uciGameService.getImportedDatabaseGameId(), gameId)) {
            uciGameService.setImportedDatabaseGameId(null);
        }
    }

    /** Returns Chess960-aware continuations for the selected analysis position. */
    public ChessDatabaseDtos.PositionResult getPositionStatistics(int ply)
            throws SQLException, IOException, NoMoveFoundException {
        List<Move> analysisMoves = uciGameService.getAnalysisMoveListSnapshot();
        ChessStartingPosition startingPosition = uciGameService.getAnalysisStartingPosition();
        DummyGame codecGame = Simulation.createDummySimulation(startingPosition);
        List<String> uciMoves = analysisMoves.stream()
                .map(move -> UciMoveCodec.encode(codecGame, move))
                .toList();

        int safePly = Math.max(0, Math.min(ply, uciMoves.size()));
        PositionStatistics statistics = database().findPosition(
                startingPosition.getId(),
                uciMoves,
                safePly);

        DummyGame notationGame = Simulation.createDummySimulation(startingPosition);
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
        long games = moves.stream().mapToLong(ChessDatabaseDtos.PositionMove::games).sum();
        return new ChessDatabaseDtos.PositionResult(safePly, games, moves);
    }

    @PreDestroy
    public void shutdown() {
        String activeId = activeImportId.get();
        if (activeId != null) {
            ImportJobState job = importJobs.get(activeId);
            if (job != null) job.requestCancellation();
        }
        importExecutor.shutdownNow();
    }

    private void runImport(ImportJobState job, Path temporaryFile) {
        try (InputStream inputStream = temporaryFileStore.open(temporaryFile)) {
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
                temporaryFileStore.deleteIfExists(temporaryFile);
            } catch (IOException ignored) {
                // Temporary file cleanup must not alter the final import state.
            }
            activeImportId.compareAndSet(job.id, null);
        }
    }

    private ImportJobState requireImportJob(String importId) {
        ImportJobState job = importJobs.get(importId);
        if (job == null) {
            throw new NoSuchElementException("Chess database import job not found: " + importId);
        }
        return job;
    }

    private String toSan(DummyGame game, String uciMove) throws IOException {
        try {
            Move move = LegalMoveResolver.resolveUci(game, uciMove);
            return PgnNotation.toDisplayNotation(game, move);
        } catch (NoMoveFoundException e) {
            return uciMove;
        }
    }

    private ChessDatabase database() throws SQLException, IOException {
        ChessDatabase current = database;
        if (current != null) return current;
        synchronized (this) {
            if (database == null) database = databaseFactory.open(databasePath);
            return database;
        }
    }

    private static ExecutorService createImportExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "chess-database-import");
            thread.setDaemon(true);
            return thread;
        });
    }

    @FunctionalInterface
    interface DatabaseFactory {
        ChessDatabase open(Path path) throws SQLException, IOException;
    }

    private enum ImportPhase {
        READING_PGN,
        FINALIZING_DATABASE,
        COMPLETE,
        CANCELLED,
        FAILED
    }

    private static final class ImportJobState {
        private final String id;
        private final String fileName;
        private final long totalBytes;
        private final LongSupplier nanoTime;
        private final long startedNanos;
        private volatile String status = "RUNNING";
        private volatile ImportPhase phase = ImportPhase.READING_PGN;
        private volatile long bytesRead;
        private volatile long processedGames;
        private volatile long importedGames;
        private volatile long skippedGames;
        private volatile long totalPlies;
        private volatile long elapsedMillis;
        private volatile String message;
        private volatile boolean cancellationRequested;

        private ImportJobState(
                String id,
                String fileName,
                long totalBytes,
                LongSupplier nanoTime) {
            this.id = id;
            this.fileName = fileName;
            this.totalBytes = totalBytes;
            this.nanoTime = nanoTime;
            this.startedNanos = nanoTime.getAsLong();
        }

        private void updateProgress(ImportProgress progress) {
            bytesRead = progress.bytesRead();
            processedGames = progress.processedGames();
            importedGames = progress.importedGames();
            skippedGames = progress.skippedGames();
            totalPlies = progress.totalPlies();
            elapsedMillis = progress.elapsedMillis();
            if ("RUNNING".equals(status)) {
                phase = totalBytes > 0L && bytesRead >= totalBytes
                        ? ImportPhase.FINALIZING_DATABASE
                        : ImportPhase.READING_PGN;
            }
        }

        private void requestCancellation() {
            if ("RUNNING".equals(status)) {
                cancellationRequested = true;
                message = "Cancellation requested…";
            }
        }

        private void complete(ImportResult result) {
            importedGames = result.importedGames();
            skippedGames = result.skippedGames();
            totalPlies = result.totalPlies();
            elapsedMillis = result.elapsedMillis();
            bytesRead = totalBytes;
            phase = ImportPhase.COMPLETE;
            message = "Import complete.";
            status = "COMPLETE";
        }

        private void cancelled() {
            elapsedMillis = elapsedSinceStartMillis();
            phase = ImportPhase.CANCELLED;
            message = "Import cancelled. No games from this import were added to the active database.";
            status = "CANCELLED";
        }

        private void failed(String failureMessage) {
            elapsedMillis = elapsedSinceStartMillis();
            phase = ImportPhase.FAILED;
            message = "Import failed: " + failureMessage;
            status = "FAILED";
        }

        private long currentElapsedMillis() {
            return "RUNNING".equals(status) ? elapsedSinceStartMillis() : elapsedMillis;
        }

        private long elapsedSinceStartMillis() {
            return Math.max(0L, (nanoTime.getAsLong() - startedNanos) / 1_000_000L);
        }

        private ChessDatabaseDtos.ImportJob snapshot() {
            return new ChessDatabaseDtos.ImportJob(
                    id,
                    fileName,
                    status,
                    phase.name(),
                    totalBytes,
                    bytesRead,
                    processedGames,
                    importedGames,
                    skippedGames,
                    totalPlies,
                    currentElapsedMillis(),
                    message);
        }
    }
}
