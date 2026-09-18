package demo.chess.api.service;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.GameSettingsDto;

/** Coordinates replacement of the active game as one backend-owned transition. */
@Service
public class GameLifecycleService {

    private final GameService gameService;
    private final EngineLifecycleCoordinator engineLifecycleCoordinator;

    public GameLifecycleService(
            GameService gameService,
            EngineLifecycleCoordinator engineLifecycleCoordinator) {
        this.gameService = gameService;
        this.engineLifecycleCoordinator = engineLifecycleCoordinator;
    }

    /**
     * Starts a new game only after every native engine belonging to the previous
     * game has been stopped and released.
     */
    public synchronized GameSettingsDto startNewGame(GameSettingsDto settings) {
        prepareForGameReplacement();
        return gameService.startNewGame(settings);
    }

    /**
     * Ends native work owned by the currently displayed game before another
     * game is selected or imported.
     */
    public void prepareForGameReplacement() {
        engineLifecycleCoordinator.stopGameScopedEngines();
    }
}
