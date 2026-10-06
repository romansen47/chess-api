package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import demo.chess.api.dto.AnalysisReplaySettingsDto;
import demo.chess.api.dto.BoardDto;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.engines.DeepAnalysisEngine;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.game.Game;

class AnalysisReplayServiceLifecycleTest {

    @Test
    void restartStopsPreviousEngineBeforeCreatingReplacement() throws Exception {
        DeepAnalysisEngine firstEngine = mock(DeepAnalysisEngine.class);
        DeepAnalysisEngine replacementEngine = mock(DeepAnalysisEngine.class);
        DeepAnalysisEngineFactory factory = mock(DeepAnalysisEngineFactory.class);
        when(factory.create("/test/engine"))
                .thenReturn(firstEngine, replacementEngine);

        AnalysisReplayService service = newService(factory);
        AnalysisReplaySettingsDto settings =
                new AnalysisReplaySettingsDto("profile", 12, 5);

        service.start(settings);
        clearInvocations(firstEngine, factory);

        service.start(settings);

        InOrder order = inOrder(firstEngine, factory);
        order.verify(firstEngine).stopEvaluation();
        order.verify(factory).create("/test/engine");
    }

    @Test
    void cancelStopsEngineBeforeReplayMonitorBecomesAvailable() throws Exception {
        DeepAnalysisEngine engine = mock(DeepAnalysisEngine.class);
        DeepAnalysisEngineFactory factory = mock(DeepAnalysisEngineFactory.class);
        when(factory.create("/test/engine")).thenReturn(engine);

        AnalysisReplayService service = newService(factory);
        service.start(new AnalysisReplaySettingsDto("profile", 12, 5));
        clearInvocations(engine);

        CountDownLatch monitorHeld = new CountDownLatch(1);
        CountDownLatch releaseMonitor = new CountDownLatch(1);
        Thread blocker = new Thread(() -> {
            synchronized (service) {
                monitorHeld.countDown();
                try {
                    releaseMonitor.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "analysis-replay-monitor-blocker");
        blocker.start();
        assertTrue(monitorHeld.await(1, TimeUnit.SECONDS));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> cancellation = executor.submit(service::cancel);

            verify(engine, timeout(500)).stopEvaluation();
            assertFalse(
                    cancellation.isDone(),
                    "Cancellation should still be waiting for replay-state cleanup");

            releaseMonitor.countDown();
            cancellation.get(1, TimeUnit.SECONDS);
        } finally {
            releaseMonitor.countDown();
            executor.shutdownNow();
            blocker.join(1000);
        }
    }

    private AnalysisReplayService newService(DeepAnalysisEngineFactory factory)
            throws Exception {
        GameService gameService = mock(GameService.class);
        EngineSettingsService engineSettingsService =
                mock(EngineSettingsService.class);
        EngineAvailabilityService engineAvailabilityService =
                mock(EngineAvailabilityService.class);
        EvaluationService evaluationService = mock(EvaluationService.class);
        UciGameService uciGameService = mock(UciGameService.class);
        AnalysisGameReplayService analysisGameReplayService =
                mock(AnalysisGameReplayService.class);
        EngineLineDisplayService engineLineDisplayService =
                mock(EngineLineDisplayService.class);

        AnalysisGameContext context = new AnalysisGameContext(
                ChessStartingPosition.STANDARD,
                List.of());
        Game replayGame = mock(Game.class);
        UciEngineConfig config = mock(UciEngineConfig.class);

        when(analysisGameReplayService.currentContext()).thenReturn(context);
        when(analysisGameReplayService.createPositionAtPly(context, 0))
                .thenReturn(replayGame);
        when(engineAvailabilityService.findAvailableDeepAnalysisProfileId(
                eq("profile"),
                any(ChessStartingPosition.class)))
                .thenReturn(Optional.of("profile"));
        when(engineSettingsService.getDeepAnalysisConfig("profile", 12, 5))
                .thenReturn(config);
        when(config.getEngine()).thenReturn("/test/engine");
        when(config.getEngineName()).thenReturn("Test Engine");
        when(gameService.getBoardView(replayGame)).thenReturn(mock(BoardDto.class));

        return new AnalysisReplayService(
                gameService,
                engineSettingsService,
                engineAvailabilityService,
                evaluationService,
                uciGameService,
                analysisGameReplayService,
                engineLineDisplayService,
                factory);
    }
}
