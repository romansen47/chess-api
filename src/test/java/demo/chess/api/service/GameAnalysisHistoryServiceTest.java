package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import demo.chess.api.dto.GameAnalysisDtos;
import demo.chess.api.dto.GameSnapshotDto;
import demo.chess.api.dto.UciGameDto;
import demo.chess.api.dto.UciGameMoveDto;
import demo.chess.database.AnalysisLine;
import demo.chess.database.AnalysisPosition;
import demo.chess.database.AnalysisRun;

class GameAnalysisHistoryServiceTest {

    @Test
    void comparesEngineOrderAndPrincipalVariationDivergence() throws Exception {
        ChessDatabaseService database = mock(ChessDatabaseService.class);
        UciGameService games = mock(UciGameService.class);
        EngineSettingsService settings = mock(EngineSettingsService.class);
        GameAnalysisHistoryService service = new GameAnalysisHistoryService(database, games, settings);
        when(games.getCurrentGameSnapshot()).thenReturn(snapshot());
        String gameKey = service.currentGameKey();

        AnalysisRun left = run(1, gameKey, "Leela", List.of(
                line(1, 0.45, "d1d5", "d8f6", "b1c3"),
                line(2, 0.31, "b1c3", "g8f6")));
        AnalysisRun right = run(2, gameKey, "Stockfish", List.of(
                line(1, 0.52, "b1c3", "g8f6"),
                line(2, 0.40, "d1d5", "d8f6", "c2c3")));
        when(database.getAnalysisRun(1)).thenReturn(left);
        when(database.getAnalysisRun(2)).thenReturn(right);

        GameAnalysisDtos.PositionComparison position =
                service.compare(1, 2).positions().get(0);

        assertTrue(position.differentBestMove());
        assertFalse(position.differentContinuation());
        assertEquals(2, position.leftBestMoveRankOnRight());
        assertEquals(2, position.rightBestMoveRankOnLeft());
        assertEquals(0, position.commonPrefixLength());
    }

    @Test
    void reportsSameRootMoveWithDifferentContinuationSeparately() throws Exception {
        ChessDatabaseService database = mock(ChessDatabaseService.class);
        UciGameService games = mock(UciGameService.class);
        EngineSettingsService settings = mock(EngineSettingsService.class);
        GameAnalysisHistoryService service = new GameAnalysisHistoryService(database, games, settings);
        when(games.getCurrentGameSnapshot()).thenReturn(snapshot());
        String gameKey = service.currentGameKey();
        when(database.getAnalysisRun(1)).thenReturn(run(1, gameKey, "A", List.of(
                line(1, 99, "d1h5", "g7g6", "h5g6"))));
        when(database.getAnalysisRun(2)).thenReturn(run(2, gameKey, "B", List.of(
                line(1, 7.2, "d1h5", "g7g6", "h5e5"))));

        GameAnalysisDtos.PositionComparison position =
                service.compare(1, 2).positions().get(0);

        assertFalse(position.differentBestMove());
        assertTrue(position.differentContinuation());
        assertEquals(2, position.commonPrefixLength());
        assertTrue(position.mateDisagreement());
        assertEquals(1, position.leftBestMoveRankOnRight());
        assertEquals(1, position.rightBestMoveRankOnLeft());
    }

    @Test
    void resolvesProfileNameForRunsStoredBeforeProfileSnapshots() throws Exception {
        ChessDatabaseService database = mock(ChessDatabaseService.class);
        UciGameService games = mock(UciGameService.class);
        EngineSettingsService settings = mock(EngineSettingsService.class);
        GameAnalysisHistoryService service = new GameAnalysisHistoryService(database, games, settings);
        when(games.getCurrentGameSnapshot()).thenReturn(snapshot());
        String gameKey = service.currentGameKey();
        AnalysisLine best = line(1, 0.2, "e2e4");
        AnalysisRun legacyRun = new AnalysisRun(
                1, gameKey, "Stockfish", "profile", "profile",
                20, 5, 1, "", "now", true,
                List.of(new AnalysisPosition(
                        1, best.evaluation(), best.depth(), best.mateDistance(), List.of(best))));
        when(database.getAnalysisRun(1)).thenReturn(legacyRun);
        when(settings.getProfileName("profile")).thenReturn("Tournament profile");

        assertEquals("Tournament profile", service.getRun(1).summary().profileName());
    }

    private GameSnapshotDto snapshot() {
        UciGameDto game = new UciGameDto(
                2, "WHITE", "", List.of(
                        new UciGameMoveDto(1, "e2e4", "e4", ""),
                        new UciGameMoveDto(2, "e7e5", "e5", "")),
                "White", "Black");
        return new GameSnapshotDto(true, game);
    }

    private AnalysisRun run(long id, String gameKey, String engine, List<AnalysisLine> lines) {
        AnalysisLine best = lines.get(0);
        return new AnalysisRun(
                id, gameKey, engine, "profile", engine + " profile",
                20, 5, lines.size(), "", "now", false,
                List.of(new AnalysisPosition(
                        18, best.evaluation(), best.depth(), best.mateDistance(), lines)));
    }

    private AnalysisLine line(int rank, double evaluation, String... moves) {
        Integer mate = Math.abs(evaluation) >= 99 ? 3 : null;
        return new AnalysisLine(rank, evaluation, 20, mate, List.of(moves));
    }
}
