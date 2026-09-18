package demo.chess.api.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class EngineLifecycleCoordinatorTest {

    @Test
    void stopsEveryGameScopedNativeEngineOwner() {
        ComputerMoveService computerMoveService = mock(ComputerMoveService.class);
        EvaluationService evaluationService = mock(EvaluationService.class);
        AnalysisEvaluationService analysisEvaluationService = mock(AnalysisEvaluationService.class);
        AnalysisReplayService analysisReplayService = mock(AnalysisReplayService.class);

        EngineLifecycleCoordinator coordinator = new EngineLifecycleCoordinator(
                computerMoveService,
                evaluationService,
                analysisEvaluationService,
                analysisReplayService);

        coordinator.stopGameScopedEngines();

        InOrder order = inOrder(
                analysisReplayService,
                analysisEvaluationService,
                evaluationService,
                computerMoveService);
        order.verify(analysisReplayService).clear();
        order.verify(analysisEvaluationService).stopEvaluation();
        order.verify(evaluationService).resetForNewGame();
        order.verify(computerMoveService).resetForNewGame();
    }
}
