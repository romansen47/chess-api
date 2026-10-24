package demo.chess.api.service;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.GameSettingsDto;
import demo.chess.api.dto.OpeningDto;
import demo.chess.definitions.ChessStartingPosition;

/** Coordinates replacement of the active game as one backend-owned transition. */
@Service
public class GameLifecycleService {

    private final GameService gameService;
    private final EngineLifecycleCoordinator engineLifecycleCoordinator;
    private final OpeningLibraryService openingLibraryService;

    public GameLifecycleService(
            GameService gameService,
            EngineLifecycleCoordinator engineLifecycleCoordinator,
            OpeningLibraryService openingLibraryService) {
        this.gameService = gameService;
        this.engineLifecycleCoordinator = engineLifecycleCoordinator;
        this.openingLibraryService = openingLibraryService;
    }

    /**
     * Starts a new game only after every native engine belonging to the previous
     * game has been stopped and released.
     */
    public synchronized GameSettingsDto startNewGame(GameSettingsDto settings) {
        prepareForGameReplacement();

        GameSettingsDto requested = settings != null ? settings : gameService.getGameSettings();
        String openingId = requested != null ? requested.getOpeningId() : null;
        if (openingId == null || openingId.isBlank()) {
            return gameService.startNewGame(settings);
        }

        OpeningDto opening = openingLibraryService.getOpening(openingId.trim());
        if (!opening.isPlayable()) {
            throw new IllegalArgumentException("Selected opening cannot be continued");
        }
        return gameService.startNewGame(
                settings,
                ChessStartingPosition.of(opening.getStartingPositionId()),
                opening.getMoves());
    }

    /**
     * Ends native work owned by the currently displayed game before another
     * game is selected or imported.
     */
    public void prepareForGameReplacement() {
        engineLifecycleCoordinator.stopGameScopedEngines();
    }
}
