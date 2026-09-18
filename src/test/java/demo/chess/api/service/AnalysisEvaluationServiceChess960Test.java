package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.impl.EvaluationUciEngine;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.game.impl.Simulation;
import demo.chess.notation.PgnNotation;

class AnalysisEvaluationServiceChess960Test {

    @Test
    void evaluatesHistoricalPositionAfterChess960CastlingWithoutFallingBackTo518()
            throws Exception {
        ChessStartingPosition startingPosition = ChessStartingPosition.of(3);
        Simulation original = Simulation.createSimulation(startingPosition);
        Move castling = PgnNotation.resolveSan(original, "O-O");

        UciGameService uciGameService = mock(UciGameService.class);
        when(uciGameService.getAnalysisGameContext()).thenReturn(
                new AnalysisGameContext(startingPosition, List.of(castling)));
        AnalysisGameReplayService replayService =
                new AnalysisGameReplayService(uciGameService);

        AnalysisVariationService variationService = mock(AnalysisVariationService.class);
        EngineRuntimeSelectionService runtime = mock(EngineRuntimeSelectionService.class);
        EngineLineDisplayService display = mock(EngineLineDisplayService.class);
        AnalysisEvaluationEngineFactory factory = mock(AnalysisEvaluationEngineFactory.class);
        AnalysisMoveAssessmentService assessment = mock(AnalysisMoveAssessmentService.class);
        EvaluationUciEngine engine = mock(EvaluationUciEngine.class);

        UciEngineConfig config = new UciEngineConfig(
                "fake-engine",
                "Fake Engine",
                "",
                Map.of());
        when(runtime.requireEvaluationConfig()).thenReturn(config);
        when(runtime.getEvaluationVersion()).thenReturn(1L);
        when(factory.create("fake-engine", "analysis evaluation")).thenReturn(engine);

        AtomicReference<Game> evaluated = new AtomicReference<>();
        when(engine.getBestLines(any(Game.class), any())).thenAnswer(invocation -> {
            evaluated.set(invocation.getArgument(0));
            return List.of();
        });

        AnalysisEvaluationService service = new AnalysisEvaluationService(
                replayService,
                variationService,
                runtime,
                display,
                factory,
                assessment);

        service.getEvaluation(1);

        assertNotNull(evaluated.get());
        assertEquals(3, evaluated.get().getStartingPosition().getId());
        assertEquals(1, evaluated.get().getMoveList().size());
    }
}
