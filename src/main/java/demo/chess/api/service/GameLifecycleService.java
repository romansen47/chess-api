package demo.chess.api.service;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.GameSettingsDto;

@Service
public class GameLifecycleService {

    private final GameService gameService;
    private final ComputerMoveService computerMoveService;
    private final EvaluationService evaluationService;

    /**
     * Creates a new GameLifecycleService instance.
     * @param gameService the game service
     * @param computerMoveService the computer move service
     * @param evaluationService the evaluation service
     */
    public GameLifecycleService(
            GameService gameService,
            ComputerMoveService computerMoveService,
            EvaluationService evaluationService) {
        this.gameService = gameService;
        this.computerMoveService = computerMoveService;
        this.evaluationService = evaluationService;
    }

    /**
     * Starts the new game.
     * @param settings the settings
     * @return the result of the operation
     */
    public synchronized GameSettingsDto startNewGame(GameSettingsDto settings) {
        computerMoveService.resetForNewGame();
        evaluationService.resetForNewGame();
        return gameService.startNewGame(settings);
    }
}
