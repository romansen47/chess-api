package demo.chess.api.service;

import org.springframework.stereotype.Service;

/**
 * Central lifecycle boundary for native engines whose work belongs to the
 * currently selected game.
 *
 * <p>A backend new-game request must close every game-scoped native engine even
 * when the browser is disconnected or unable to issue its own cleanup calls.</p>
 */
@Service
public class EngineLifecycleCoordinator {

    private final ComputerMoveService computerMoveService;
    private final EvaluationService evaluationService;
    private final AnalysisEvaluationService analysisEvaluationService;
    private final AnalysisReplayService analysisReplayService;

    public EngineLifecycleCoordinator(
            ComputerMoveService computerMoveService,
            EvaluationService evaluationService,
            AnalysisEvaluationService analysisEvaluationService,
            AnalysisReplayService analysisReplayService) {
        this.computerMoveService = computerMoveService;
        this.evaluationService = evaluationService;
        this.analysisEvaluationService = analysisEvaluationService;
        this.analysisReplayService = analysisReplayService;
    }

    /** Stops and releases all native engine instances tied to the previous game. */
    public void stopGameScopedEngines() {
        analysisReplayService.clear();
        analysisEvaluationService.stopEvaluation();
        evaluationService.resetForNewGame();
        computerMoveService.resetForNewGame();
    }
}
