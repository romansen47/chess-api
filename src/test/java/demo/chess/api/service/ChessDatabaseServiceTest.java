package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import demo.chess.api.dto.ChessDatabaseDtos;
import demo.chess.database.ChessDatabase;
import demo.chess.database.ImportCancelledException;
import demo.chess.database.ImportProgress;
import demo.chess.database.ImportResult;
import demo.chess.load.GameLoader;

class ChessDatabaseServiceTest {

    @Test
    void completedImportPublishesProgressAndDeletesTemporaryFile() throws Exception {
        ChessDatabase database = mock(ChessDatabase.class);
        ImportTemporaryFileStore files = stagedFileStore();
        DirectExecutor executor = new DirectExecutor();
        when(database.importPgn(
                anyString(),
                any(InputStream.class),
                anyLong(),
                any(),
                any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Consumer<ImportProgress> progress = invocation.getArgument(3);
                    progress.accept(new ImportProgress(3, 3, 1, 1, 0, 8, 4));
                    return new ImportResult(1, 0, 8, 5);
                });
        ChessDatabaseService service = newService(database, executor, files);

        ChessDatabaseDtos.ImportJob job = service.startImport(
                "one.pgn",
                new ByteArrayInputStream("pgn".getBytes()));

        assertEquals("COMPLETE", job.status());
        assertEquals("COMPLETE", job.phase());
        assertEquals(1, job.importedGames());
        assertEquals(8, job.totalPlies());
        verify(files).deleteIfExists(Path.of("staged.pgn"));
        service.shutdown();
    }

    @Test
    void cancellationIsForwardedCooperativelyAndReleasesImportSlot() throws Exception {
        ChessDatabase database = mock(ChessDatabase.class);
        ImportTemporaryFileStore files = stagedFileStore();
        CapturingExecutor executor = new CapturingExecutor();
        when(database.importPgn(
                anyString(),
                any(InputStream.class),
                anyLong(),
                any(),
                any()))
                .thenAnswer(invocation -> {
                    BooleanSupplier cancelled = invocation.getArgument(4);
                    if (cancelled.getAsBoolean()) {
                        throw new ImportCancelledException("cancelled");
                    }
                    return new ImportResult(1, 0, 8, 5);
                });
        ChessDatabaseService service = newService(database, executor, files);
        ChessDatabaseDtos.ImportJob started = service.startImport(
                "large.pgn",
                new ByteArrayInputStream("pgn".getBytes()));

        ChessDatabaseDtos.ImportJob cancelling = service.cancelImport(started.id());
        assertEquals("RUNNING", cancelling.status());
        executor.runCaptured();

        ChessDatabaseDtos.ImportJob cancelled = service.getImportJob(started.id());
        assertEquals("CANCELLED", cancelled.status());
        assertEquals("CANCELLED", cancelled.phase());
        verify(files).deleteIfExists(Path.of("staged.pgn"));

        service.startImport("next.pgn", new ByteArrayInputStream("pgn".getBytes()));
        service.shutdown();
    }

    @Test
    void deletingSelectedDatabaseGameAlsoClearsItsSelection() throws Exception {
        ChessDatabase database = mock(ChessDatabase.class);
        UciGameService games = mock(UciGameService.class);
        when(games.getImportedDatabaseGameId()).thenReturn(42L);
        ChessDatabaseService service = newService(
                games,
                database,
                new DirectExecutor(),
                stagedFileStore());

        service.deleteGame(42L);

        verify(database).deleteGame(42L);
        verify(games).setImportedDatabaseGameId(null);
        service.shutdown();
    }

    @Test
    void deletingUnselectedGamePreservesCurrentSelection() throws Exception {
        ChessDatabase database = mock(ChessDatabase.class);
        UciGameService games = mock(UciGameService.class);
        when(games.getImportedDatabaseGameId()).thenReturn(7L);
        ChessDatabaseService service = newService(
                games,
                database,
                new DirectExecutor(),
                stagedFileStore());

        service.deleteGame(42L);

        verify(database).deleteGame(42L);
        verify(games, never()).setImportedDatabaseGameId(any());
        service.shutdown();
    }

    private ChessDatabaseService newService(
            ChessDatabase database,
            AbstractExecutorService executor,
            ImportTemporaryFileStore files) {
        return newService(mock(UciGameService.class), database, executor, files);
    }

    private ChessDatabaseService newService(
            UciGameService games,
            ChessDatabase database,
            AbstractExecutorService executor,
            ImportTemporaryFileStore files) {
        AtomicLong nanos = new AtomicLong(1_000_000L);
        return new ChessDatabaseService(
                games,
                mock(DatabaseGameSearchMapper.class),
                Path.of("database.sqlite"),
                ignored -> database,
                executor,
                files,
                () -> nanos.getAndAdd(1_000_000L),
                new GameLoader(),
                new ChessAnalysisDiagnosticPgnSanitizer());
    }

    private ImportTemporaryFileStore stagedFileStore() throws Exception {
        ImportTemporaryFileStore files = mock(ImportTemporaryFileStore.class);
        Path path = Path.of("staged.pgn");
        when(files.stage(any(InputStream.class))).thenReturn(path);
        when(files.size(path)).thenReturn(3L);
        when(files.open(path)).thenReturn(new ByteArrayInputStream("pgn".getBytes()));
        return files;
    }

    private static class DirectExecutor extends AbstractExecutorService {
        private boolean shutdown;

        @Override
        public void shutdown() { shutdown = true; }

        @Override
        public List<Runnable> shutdownNow() { shutdown = true; return List.of(); }

        @Override
        public boolean isShutdown() { return shutdown; }

        @Override
        public boolean isTerminated() { return shutdown; }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) { return shutdown; }

        @Override
        public void execute(Runnable command) { command.run(); }
    }

    private static final class CapturingExecutor extends DirectExecutor {
        private Runnable captured;

        @Override
        public void execute(Runnable command) { captured = command; }

        void runCaptured() {
            Runnable command = captured;
            captured = null;
            if (command != null) command.run();
        }
    }
}
