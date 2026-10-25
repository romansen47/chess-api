package demo.chess.api.service;

import java.io.IOException;
import java.sql.SQLException;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.GameMetadataDto;
import demo.chess.definitions.engines.impl.NoMoveFoundException;

/** Coordinates persisted PGN metadata changes with the selected analysis context. */
@Service
public class GameMetadataService {

    private final UciGameService uciGameService;
    private final ChessDatabaseService chessDatabaseService;

    public GameMetadataService(
            UciGameService uciGameService,
            ChessDatabaseService chessDatabaseService) {
        this.uciGameService = uciGameService;
        this.chessDatabaseService = chessDatabaseService;
    }

    public synchronized GameMetadataDto updateMetadata(GameMetadataDto request)
            throws NoMoveFoundException, IOException, SQLException {
        Long gameId = uciGameService.getImportedDatabaseGameId();
        if (gameId == null) {
            throw new IllegalStateException("The selected analysis game is not stored in the database.");
        }

        UciGameService.PreparedMetadataUpdate prepared =
                uciGameService.prepareMetadataUpdate(request == null ? null : request.getTags());
        chessDatabaseService.saveAnnotatedPgn(gameId, prepared.pgn());
        uciGameService.applyMetadataUpdate(prepared);
        return new GameMetadataDto(prepared.tags());
    }
}
