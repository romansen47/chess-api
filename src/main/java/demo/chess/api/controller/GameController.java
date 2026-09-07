package demo.chess.api.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import demo.chess.api.service.ChessDatabaseService;
import demo.chess.api.service.GameLifecycleService;
import demo.chess.api.service.GameService;
import demo.chess.api.service.UciGameService;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.load.GameLoader;

@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService gameService;
    private final GameLifecycleService gameLifecycleService;
    private final UciGameService uciGameService;
    private final AnalysisReplayService analysisReplayService;
    private final ChessDatabaseService chessDatabaseService;
    private final GameLoader gameLoader = new GameLoader();

    /**
     * Creates a new GameController instance.
     * @param gameService the game service
     * @param gameLifecycleService the game lifecycle service
     * @param uciGameService the uci game service
     * @param analysisReplayService the analysis replay service
     * @param chessDatabaseService the local chess database service
     */
    public GameController(
            GameService gameService,
            GameLifecycleService gameLifecycleService,
            UciGameService uciGameService,
            AnalysisReplayService analysisReplayService,
            ChessDatabaseService chessDatabaseService) {
        this.gameService = gameService;
        this.gameLifecycleService = gameLifecycleService;
        this.uciGameService = uciGameService;
        this.analysisReplayService = analysisReplayService;
        this.chessDatabaseService = chessDatabaseService;
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
     * Imports exactly one PGN game for analysis and stores it in the local database.
     *
     * @param content complete PGN content
     * @return imported analysis game
     */
    @PostMapping(
            value = "/game/pgn",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importPgnGame(@RequestBody(required = false) String content) {
        List<String> games = gameLoader.splitPgnGames(content);
        if (games.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "PGN_NO_GAME",
                    "gameCount", 0));
        }
        if (games.size() > 1) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "PGN_MULTIPLE_GAMES",
                    "gameCount", games.size()));
        }

        String singleGame = games.get(0);
        analysisReplayService.cancel();

        try {
            chessDatabaseService.importSingleGame(singleGame);
            UciGameDto importedGame = uciGameService.importGame(singleGame);
            return ResponseEntity.ok(importedGame);
        } catch (NoMoveFoundException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body("Database error while importing PGN game");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("I/O error while importing PGN game");
        }
    }
}
