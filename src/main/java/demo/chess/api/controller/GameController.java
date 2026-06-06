package demo.chess.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.GameSettingsDto;
import demo.chess.api.service.GameLifecycleService;
import demo.chess.api.service.GameService;

@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService gameService;
    private final GameLifecycleService gameLifecycleService;

    public GameController(GameService gameService, GameLifecycleService gameLifecycleService) {
        this.gameService = gameService;
        this.gameLifecycleService = gameLifecycleService;
    }

    /**
     * Liefert die aktuell verwendeten Einstellungen für neue Partien.
     */
    @GetMapping("/game-settings")
    public ResponseEntity<GameSettingsDto> getGameSettings() {
        return ResponseEntity.ok(gameService.getGameSettings());
    }

    /**
     * Startet eine neue Partie mit den vom Frontend gewählten Einstellungen.
     */
    @PostMapping("/new-game")
    public ResponseEntity<GameSettingsDto> startNewGame(@RequestBody(required = false) GameSettingsDto settings) {
        GameSettingsDto appliedSettings = gameLifecycleService.startNewGame(settings);
        return ResponseEntity.ok(appliedSettings);
    }
}
