package demo.chess.api.controller;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.GameSettingsDto;
import demo.chess.api.dto.UciGameDto;
import demo.chess.api.service.AnalysisReplayService;
import demo.chess.api.service.GameLifecycleService;
import demo.chess.api.service.GameService;
import demo.chess.api.service.UciGameService;
import demo.chess.definitions.engines.impl.NoMoveFoundException;

@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService gameService;
    private final GameLifecycleService gameLifecycleService;
    private final UciGameService uciGameService;
    private final AnalysisReplayService analysisReplayService;

    public GameController(
            GameService gameService,
            GameLifecycleService gameLifecycleService,
            UciGameService uciGameService,
            AnalysisReplayService analysisReplayService) {
        this.gameService = gameService;
        this.gameLifecycleService = gameLifecycleService;
        this.uciGameService = uciGameService;
        this.analysisReplayService = analysisReplayService;
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
        uciGameService.clearImportedGame();
        return ResponseEntity.ok(appliedSettings);
    }

    /**
     * Exportiert die aktuelle Analysequelle als UCI-Zugliste. Wenn zuvor eine UCI-Datei
     * geladen wurde, wird diese Partie exportiert; ansonsten die aktuell gespielte Partie.
     */
    @GetMapping(value = "/game/uci", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> exportUciGame() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"game.uci\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(uciGameService.exportGame());
    }

    /**
     * Lädt eine UCI-Zugliste als separate, ruhende Analysepartie. Das aktuell laufende
     * Game im GameService wird dabei nicht überschrieben.
     */
    @PostMapping(
            value = "/game/uci",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importUciGame(@RequestBody(required = false) String content) {
        analysisReplayService.cancel();

        try {
            UciGameDto importedGame = uciGameService.importGame(content);
            return ResponseEntity.ok(importedGame);
        } catch (NoMoveFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("I/O error while importing UCI game");
        }
    }
}
