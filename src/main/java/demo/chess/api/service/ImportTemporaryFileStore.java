package demo.chess.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Stages uploaded PGN streams so database imports can run asynchronously. */
class ImportTemporaryFileStore {

    Path stage(InputStream inputStream) throws IOException {
        Path temporaryFile = Files.createTempFile("chess-database-import-", ".pgn");
        try {
            Files.copy(inputStream, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            return temporaryFile;
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(temporaryFile);
            throw e;
        }
    }

    long size(Path path) throws IOException {
        return Files.size(path);
    }

    InputStream open(Path path) throws IOException {
        return Files.newInputStream(path);
    }

    void deleteIfExists(Path path) throws IOException {
        Files.deleteIfExists(path);
    }
}
