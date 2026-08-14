package demo.chess.api.service;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.GameSettingsDto;

@Service
public class GameLifecycleService {

    private final GameService gameService;
    private final ComputerMoveService computerMoveService;
    private final EvaluationService evaluationService;

    public GameLifecycleService(
            GameService gameService,
            ComputerMoveService computerMoveService,
            EvaluationService evaluationService) {
        this.gameService = gameService;
        this.computerMoveService = computerMoveService;
        this.evaluationService = evaluationService;
    }

    /**
     * Starts a new game with a clean engine lifecycle boundary.
     *
     * Existing player engines are stopped before the GameService swaps the current game
     * instance and stay dormant until a computer move actually needs them. The evaluation
     * engine is stopped and stays dormant until the next evaluation request.
     * Player-engine reset is deliberately able to
     * cancel an in-flight computer move instead of waiting for it to finish.
     * GameService.applyMoveIfCurrent(...) protects the new game from stale results.
     */
    public synchronized GameSettingsDto startNewGame(GameSettingsDto settings) {
        computerMoveService.resetForNewGame();
        evaluationService.resetForNewGame();
        return gameService.startNewGame(settings);
    }
}
