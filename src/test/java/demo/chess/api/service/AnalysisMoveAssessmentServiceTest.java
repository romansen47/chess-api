package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

import demo.chess.analysis.annotation.MoveAnnotationKind;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.impl.EvaluationUciEngine;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

class AnalysisMoveAssessmentServiceTest {

    @Test
    void classifiesWithMockedLiveEngineWithoutRealEngineDependency()
            throws Exception {
        TestContext context = contextWithSingleMove();

        List<EngineLine> finalLines = List.of(
                line(3.0, 20, "d2d4"),
                line(0.0, 20, "g1f3"),
                line(-3.0, 20, "e2e4"));
        when(context.engine.getBestLines(any(Game.class), any()))
                .thenReturn(finalLines);

        AnalysisMoveAssessmentService.Result result =
                context.service.assess(1, -3.0);

        assertTrue(result.ready());
        assertEquals(20, result.depth());
        assertNotNull(result.annotation());
        assertEquals(
                MoveAnnotationKind.BLUNDER,
                result.annotation().getKind());
    }

    @Test
    void classifiesTemporaryVariationMoveWithoutPersistedGameMove()
            throws Exception {
        TestContext context = contextWithSingleMove();

        Simulation variationRoot = Simulation.createSimulation();
        Move e4 = LegalMoveResolver.resolveUci(variationRoot, "e2e4");
        variationRoot.apply(e4);
        Move e5 = LegalMoveResolver.resolveUci(variationRoot, "e7e5");
        variationRoot.apply(e5);

        List<EngineLine> finalLines = List.of(
                line(3.0, 20, "d2d4"),
                line(0.0, 20, "c2c3"),
                line(-3.0, 20, "g1f3"));
        when(context.engine.getBestLines(any(Game.class), any()))
                .thenReturn(finalLines);

        AnalysisMoveAssessmentService.Result result =
                context.service.assessPosition(
                        variationRoot,
                        "g1f3",
                        "variation:2:g1f3",
                        -3.0);

        assertTrue(result.ready());
        assertEquals(20, result.depth());
        assertNotNull(result.annotation());
        assertEquals(
                MoveAnnotationKind.BLUNDER,
                result.annotation().getKind());
    }

    @Test
    void accumulatesLiveDepthSnapshotsForCoreDeepDiscovery()
            throws Exception {
        TestContext context = contextWithSingleMove();

        List<EngineLine> finalLines = List.of(
                line(1.5, 20, "e2e4 e7e5"),
                line(1.0, 20, "d2d4 d7d5"),
                line(0.8, 20, "g1f3 g8f6"));
        when(context.engine.getBestLines(any(Game.class), any()))
                .thenReturn(finalLines);

        AnalysisMoveAssessmentService.Result initial =
                context.service.assess(1, 1.5);
        assertTrue(initial.ready());

        BiConsumer<String, List<EngineLine>> listener =
                context.listener.get();
        assertNotNull(listener);

        listener.accept("[]", List.of(
                line(0.5, 5, "d2d4"),
                line(0.3, 5, "g1f3"),
                line(-1.0, 5, "e2e4")));
        listener.accept("[]", List.of(
                line(0.5, 6, "d2d4"),
                line(0.3, 6, "g1f3"),
                line(-1.0, 6, "e2e4")));
        listener.accept("[]", List.of(
                line(0.6, 8, "d2d4"),
                line(0.4, 8, "g1f3"),
                line(-0.9, 8, "e2e4")));
        listener.accept("[]", List.of(
                line(0.7, 10, "d2d4"),
                line(-0.1, 10, "e2e4"),
                line(-0.2, 10, "g1f3")));
        listener.accept("[]", List.of(
                line(0.8, 12, "d2d4"),
                line(0.0, 12, "e2e4"),
                line(-0.2, 12, "g1f3")));
        listener.accept("[]", List.of(
                line(1.2, 15, "e2e4"),
                line(1.0, 15, "d2d4"),
                line(0.8, 15, "g1f3")));
        listener.accept("[]", List.of(
                line(1.4, 18, "e2e4"),
                line(1.0, 18, "d2d4"),
                line(0.8, 18, "g1f3")));
        listener.accept("[]", finalLines);

        AnalysisMoveAssessmentService.Result result =
                context.service.assess(1, 1.5);

        assertTrue(result.ready());
        assertNotNull(result.annotation());
        assertEquals(
                MoveAnnotationKind.BRILLIANT,
                result.annotation().getKind());
    }

    private TestContext contextWithSingleMove() throws Exception {
        UciGameService uciGameService = mock(UciGameService.class);
        EngineRuntimeSelectionService runtime =
                mock(EngineRuntimeSelectionService.class);
        AnalysisEvaluationEngineFactory factory =
                mock(AnalysisEvaluationEngineFactory.class);
        EvaluationUciEngine engine = mock(EvaluationUciEngine.class);

        Simulation original = Simulation.createSimulation();
        Move e4 = LegalMoveResolver.resolveUci(original, "e2e4");
        original.apply(e4);
        when(uciGameService.getAnalysisMoveListSnapshot())
                .thenReturn(List.of(e4));

        UciEngineConfig config = new UciEngineConfig(
                "fake-engine",
                "Fake Engine",
                "",
                Map.of());
        when(runtime.getEvaluationConfig()).thenReturn(config);
        when(runtime.getEvaluationEnginePath())
                .thenReturn("fake-engine");
        when(runtime.getEvaluationVersion()).thenReturn(1L);
        when(factory.create(
                "fake-engine",
                "analysis move assessment"))
                .thenReturn(engine);

        AtomicReference<BiConsumer<String, List<EngineLine>>> listener =
                new AtomicReference<>();
        doAnswer(invocation -> {
            listener.set(invocation.getArgument(0));
            return null;
        }).when(engine).setEvaluationUpdateListener(any());

        AnalysisMoveAssessmentService service =
                new AnalysisMoveAssessmentService(
                        uciGameService,
                        runtime,
                        factory);

        return new TestContext(service, engine, listener);
    }

    private EngineLine line(double evaluation, int depth, String moves) {
        return new EngineLine(evaluation, depth, null, moves);
    }

    private record TestContext(
            AnalysisMoveAssessmentService service,
            EvaluationUciEngine engine,
            AtomicReference<BiConsumer<String, List<EngineLine>>> listener) {
    }
}
