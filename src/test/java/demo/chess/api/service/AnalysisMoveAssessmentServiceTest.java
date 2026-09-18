package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

import demo.chess.api.engine.NativeEngineRole;
import demo.chess.api.exception.NativeEngineUnavailableException;
import demo.chess.analysis.annotation.MoveAnnotationKind;
import demo.chess.definitions.ChessStartingPosition;
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
    void failedEngineReplacementKeepsExistingAssessmentEngine() throws Exception {
        TestContext context = contextWithSingleMove();

        List<EngineLine> finalLines = List.of(
                line(1.0, 20, "e2e4"));
        when(context.engine.getBestLines(any(Game.class), any()))
                .thenReturn(finalLines);

        AnalysisMoveAssessmentService.Result initial =
                context.service.assess(1, 1.0);
        assertTrue(initial.ready());

        UciEngineConfig replacementConfig = new UciEngineConfig(
                "missing-engine",
                "Missing Engine",
                "",
                Map.of());
        when(context.runtime.requireEvaluationConfig())
                .thenReturn(replacementConfig);
        when(context.factory.create(
                "missing-engine",
                "analysis move assessment"))
                .thenThrow(NativeEngineUnavailableException.startFailure(
                        NativeEngineRole.EVALUATION,
                        new IllegalStateException("UCI handshake failed")));

        assertThrows(
                NativeEngineUnavailableException.class,
                () -> context.service.assess(1, 1.0));

        verify(context.engine, never()).close();
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
                MoveAnnotationKind.EXTRAORDINARY,
                result.annotation().getKind());
    }

    @Test
    void chess960HistoricalAssessmentPreservesStartingPosition() throws Exception {
        UciGameService uciGameService = mock(UciGameService.class);
        EngineRuntimeSelectionService runtime = mock(EngineRuntimeSelectionService.class);
        AnalysisEvaluationEngineFactory factory = mock(AnalysisEvaluationEngineFactory.class);
        EvaluationUciEngine engine = mock(EvaluationUciEngine.class);

        ChessStartingPosition startingPosition = ChessStartingPosition.of(0);
        Simulation original = Simulation.createSimulation(startingPosition);
        List<Move> originalMoves = new ArrayList<>();
        for (String uci : List.of("b2b3", "c7c5", "a1b2")) {
            Move move = LegalMoveResolver.resolveUci(original, uci);
            original.apply(move);
            originalMoves.add(move);
        }
        when(uciGameService.getAnalysisGameContext()).thenReturn(
                new AnalysisGameContext(startingPosition, originalMoves));

        UciEngineConfig config = new UciEngineConfig(
                "fake-engine", "Fake Engine", "", Map.of());
        when(runtime.requireEvaluationConfig()).thenReturn(config);
        when(runtime.getEvaluationVersion()).thenReturn(1L);
        when(factory.create("fake-engine", "analysis move assessment")).thenReturn(engine);

        AtomicReference<Game> evaluatedGame = new AtomicReference<>();
        when(engine.getBestLines(any(Game.class), any())).thenAnswer(invocation -> {
            evaluatedGame.set(invocation.getArgument(0));
            return List.of();
        });

        AnalysisMoveAssessmentService service = new AnalysisMoveAssessmentService(
                uciGameService, runtime, factory);

        AnalysisMoveAssessmentService.Result result = service.assess(3, 0.0);

        assertFalse(result.ready());
        assertNotNull(evaluatedGame.get());
        assertEquals(0, evaluatedGame.get().getStartingPosition().getId());
        assertEquals(2, evaluatedGame.get().getMoveList().size());
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
        when(uciGameService.getAnalysisGameContext())
                .thenReturn(new AnalysisGameContext(
                        ChessStartingPosition.STANDARD,
                        List.of(e4)));

        UciEngineConfig config = new UciEngineConfig(
                "fake-engine",
                "Fake Engine",
                "",
                Map.of());
        when(runtime.requireEvaluationConfig()).thenReturn(config);
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

        return new TestContext(service, engine, listener, runtime, factory);
    }

    private EngineLine line(double evaluation, int depth, String moves) {
        return new EngineLine(evaluation, depth, null, moves);
    }

    private record TestContext(
            AnalysisMoveAssessmentService service,
            EvaluationUciEngine engine,
            AtomicReference<BiConsumer<String, List<EngineLine>>> listener,
            EngineRuntimeSelectionService runtime,
            AnalysisEvaluationEngineFactory factory) {
    }
}
