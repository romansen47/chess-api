package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import demo.chess.api.dto.GameMetadataDto;

class GameMetadataServiceTest {

    @Test
    void persistsPreparedPgnBeforePublishingMetadataIntoAnalysisContext() throws Exception {
        UciGameService uciGameService = mock(UciGameService.class);
        ChessDatabaseService chessDatabaseService = mock(ChessDatabaseService.class);
        GameMetadataService service = new GameMetadataService(
                uciGameService,
                chessDatabaseService);

        GameMetadataDto request = new GameMetadataDto(Map.of(
                "White", "Alice",
                "Black", "Bob",
                "Site", "Stuttgart"));
        UciGameService.PreparedMetadataUpdate prepared =
                new UciGameService.PreparedMetadataUpdate(
                        Map.of("White", "Alice", "Black", "Bob", "Site", "Stuttgart"),
                        "[White \"Alice\"]\n[Black \"Bob\"]\n[Site \"Stuttgart\"]\n\n*\n");

        when(uciGameService.getImportedDatabaseGameId()).thenReturn(77L);
        when(uciGameService.prepareMetadataUpdate(request.getTags())).thenReturn(prepared);

        GameMetadataDto saved = service.updateMetadata(request);

        assertEquals("Stuttgart", saved.getTags().get("Site"));
        InOrder order = inOrder(uciGameService, chessDatabaseService);
        order.verify(uciGameService).getImportedDatabaseGameId();
        order.verify(uciGameService).prepareMetadataUpdate(request.getTags());
        order.verify(chessDatabaseService).saveAnnotatedPgn(77L, prepared.pgn());
        order.verify(uciGameService).applyMetadataUpdate(prepared);
    }
}
