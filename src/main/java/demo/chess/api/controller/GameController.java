package demo.chess.api.controller;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * Creates a new GameController instance.
     * @param gameService the game service
     * @param gameLifecycleService the game lifecycle service
     * @param uciGameService the uci game service
     * @param analysisReplayService the analysis replay service
     */
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
     * Returns the game settings.
     * @return the game settings
     */
    @GetMapping("/game-settings")
    public ResponseEntity<GameSettingsDto> getGameSettings() {
        return ResponseEntity.ok(gameService.getGameSettings());
    }

    /**
     * Starts the new game.
     * @param settings the settings
     * @return the result of the operation
     */
    @PostMapping("/new-game")
    public ResponseEntity<GameSettingsDto> startNewGame(@RequestBody(required = false) GameSettingsDto settings) {
        GameSettingsDto appliedSettings = gameLifecycleService.startNewGame(settings);
        uciGameService.clearImportedGame();
        return ResponseEntity.ok(appliedSettings);
    }

    /**
     * Performs the export pgn game operation.
     * @param whiteComputer the white computer
     * @param blackComputer the black computer
     * @return the result of the operation
     */
    @GetMapping(value = "/game/pgn", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> exportPgnGame(
            @RequestParam(name = "whiteComputer", defaultValue = "false") boolean whiteComputer,
            @RequestParam(name = "blackComputer", defaultValue = "false") boolean blackComputer) {
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"game.pgn\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(uciGameService.exportGame(whiteComputer, blackComputer));
        } catch (NoMoveFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("I/O error while exporting PGN game");
        }
    }

    /**
     * Performs the import pgn game operation.
     * @param content the content
     * @return the result of the operation
     */
    @PostMapping(
            value = "/game/pgn",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importPgnGame(@RequestBody(required = false) String content) {
        analysisReplayService.cancel();

        try {
            UciGameDto importedGame = uciGameService.importGame(content);
            return ResponseEntity.ok(importedGame);
        } catch (NoMoveFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("I/O error while importing PGN game");
        }
    }
}
