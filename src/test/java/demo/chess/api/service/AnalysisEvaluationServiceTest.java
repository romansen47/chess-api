package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import demo.chess.api.dto.AnalysisVariationRequestDto;
import demo.chess.api.dto.EngineEvaluationDto;
import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.impl.EvaluationUciEngine;
import demo.chess.game.Game;

class AnalysisEvaluationServiceTest {

    @Test
    void reusesSingleLiveEngineForVariationAnnotation()
            throws Exception {
        TestContext context = createContext();

        List<EngineLine> rootLines = rootBlunderLines();
        List<EngineLine> afterMoveLines = List.of(
                line(-3.0, 20, "e7e5"));

        when(context.engine.getBestLines(any(Game.class), any()))
                .thenAnswer(invocation -> {
                    Game game = invocation.getArgument(0);
                    return game.getMoveList().isEmpty()
                            ? rootLines
                            : afterMoveLines;
                });

        EngineEvaluationDto rootEvaluation =
                context.service.getVariationEvaluation(
                        request(0, List.of()));
        assertFalse(rootEvaluation.isMoveAnnotationReady());

        EngineEvaluationDto moveEvaluation =
                context.service.getVariationEvaluation(
                        request(0, List.of("e2e4")));

        assertTrue(moveEvaluation.isMoveAnnotationReady());
        assertNotNull(moveEvaluation.getMoveAnnotation());
        assertEquals(
                "blunder",
                moveEvaluation.getMoveAnnotation().getKind());
        assertEquals(20, moveEvaluation.getMoveAnnotationDepth());

        verify(context.factory, times(1)).create(
                "fake-engine",
                "analysis evaluation");
    }

    @Test
    void usesDeepAnalysisBootstrapWhenNoLiveSnapshotExists()
            throws Exception {
        TestContext context = createContext();

        DeepAnalysisResult fallback =
                new DeepAnalysisResult(
                        rootBlunderLines(),
                        Map.of(
                                20,
                                rootBlunderLines()));
        when(context.analysisReplayService
                .getDeepAnalysisResultForPly(0))
                .thenReturn(fallback);
        when(context.engine.getBestLines(any(Game.class), any()))
                .thenReturn(List.of(
                        line(-3.0, 20, "e7e5")));

        EngineEvaluationDto result =
                context.service.getVariationEvaluation(
                        request(0, List.of("e2e4")));

        assertTrue(result.isMoveAnnotationReady());
        assertNotNull(result.getMoveAnnotation());
        assertEquals(
                "blunder",
                result.getMoveAnnotation().getKind());
        verify(context.factory, times(1)).create(
                "fake-engine",
                "analysis evaluation");
    }

    @Test
    void leavesAnnotationPendingWithoutLiveOrDeepAnalysisSnapshot()
            throws Exception {
        TestContext context = createContext();

        when(context.engine.getBestLines(any(Game.class), any()))
                .thenReturn(List.of(
                        line(-3.0, 20, "e7e5")));

        EngineEvaluationDto result =
                context.service.getVariationEvaluation(
                        request(0, List.of("e2e4")));

        assertFalse(result.isMoveAnnotationReady());
        verify(context.factory, times(1)).create(
                "fake-engine",
                "analysis evaluation");
    }

    private TestContext createContext() throws Exception {
        UciGameService uciGameService =
                mock(UciGameService.class);
        when(uciGameService.getAnalysisMoveListSnapshot())
                .thenReturn(List.of());

        AnalysisVariationService variationService =
                new AnalysisVariationService(uciGameService);
        EngineRuntimeSelectionService runtime =
                mock(EngineRuntimeSelectionService.class);
        AnalysisEvaluationEngineFactory factory =
                mock(AnalysisEvaluationEngineFactory.class);
        AnalysisReplayService replayService =
                mock(AnalysisReplayService.class);
        EvaluationUciEngine engine =
                mock(EvaluationUciEngine.class);

        UciEngineConfig config =
                new UciEngineConfig(
                        "fake-engine",
                        "Fake Engine",
                        "",
                        Map.of());

        when(runtime.getEvaluationConfig())
                .thenReturn(config);
        when(runtime.getEvaluationEnginePath())
                .thenReturn("fake-engine");
        when(runtime.getEvaluationEngineName())
                .thenReturn("Fake Engine");
        when(runtime.getEvaluationVersion())
                .thenReturn(1L);
        when(factory.create(
                "fake-engine",
                "analysis evaluation"))
                .thenReturn(engine);

        AnalysisEvaluationService service =
                new AnalysisEvaluationService(
                        uciGameService,
                        variationService,
                        runtime,
                        new EngineLineDisplayService(),
                        factory,
                        replayService);

        return new TestContext(
                service,
                engine,
                factory,
                replayService);
    }

    private AnalysisVariationRequestDto request(
            int anchorPly,
            List<String> moves) {
        AnalysisVariationRequestDto request =
                new AnalysisVariationRequestDto();
        request.setAnchorPly(anchorPly);
        request.setMoves(moves);
        return request;
    }

    private List<EngineLine> rootBlunderLines() {
        return List.of(
                line(3.0, 20, "d2d4 d7d5"),
                line(0.0, 20, "g1f3 g8f6"),
                line(-3.0, 20, "e2e4 e7e5"));
    }

    private EngineLine line(
            double evaluation,
            int depth,
            String moves) {
        return new EngineLine(
                evaluation,
                depth,
                null,
                moves);
    }

    private record TestContext(
            AnalysisEvaluationService service,
            EvaluationUciEngine engine,
            AnalysisEvaluationEngineFactory factory,
            AnalysisReplayService analysisReplayService) {
    }
}
