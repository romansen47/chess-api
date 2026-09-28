package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import demo.chess.api.dto.GameAnnotationDto;
import demo.chess.notation.PgnAnnotationParser;

class UciGameTimeAnnotationsTest {
    @Test
    void survivesImportCommentEditSnapshotAndExport() throws Exception {
        UciGameService service = new UciGameService(new GameService(), null);
        var imported = service.importGame("{Introduction} 1. e4 {Note [%clk 0:05:00.125] [%emt 0:00:02]} e5 {[%clk 0:00:00]} *", 42L);
        var first = imported.getAnnotations().get(1);
        assertEquals(1, first.ply());
        assertEquals(300125L, first.clockMillis());
        assertEquals(2000L, first.elapsedMoveMillis());
        var edited = new GameAnnotationDto(first.ply(), first.nag(), "Edited", first.evaluation(), first.variations(),
                first.clockMillis(), first.elapsedMoveMillis());
        service.updateAnnotations(List.of(imported.getAnnotations().get(0), edited, imported.getAnnotations().get(2)), false, false);
        var snapshot = service.getCurrentGameSnapshot();
        assertEquals(42L, snapshot.getGame().getDatabaseGameId());
        assertEquals(300125L, snapshot.getGame().getAnnotations().get(1).clockMillis());
        var reloaded = new PgnAnnotationParser().parse(service.exportGame(false, false));
        assertEquals("Introduction", reloaded.get(0).comment());
        assertEquals("Edited", reloaded.get(1).comment());
        assertEquals(300125L, reloaded.get(1).clockMillis());
        assertEquals(0L, reloaded.get(2).clockMillis());
    }

    @Test
    void diagnosticImportKeepsClockTagsInDisplayedAndExportedGame() throws Exception {
        UciGameService service = new UciGameService(new GameService(), null);
        String pgn = "[AnalysisFormat \"ChessAnalysisTool-Diagnostic-v2\"]\n\n1. e4 {[%clk 0:05:00] [%emt 0:00:02] [%eval 0.2] class=GOOD} e5 *";
        var game = service.importGame(pgn);
        assertEquals(300000L, game.getAnnotations().getFirst().clockMillis());
        assertEquals(2000L, game.getAnnotations().getFirst().elapsedMoveMillis());
        assertFalse(service.exportGame(false, false).contains("class=GOOD"));
    }
}
