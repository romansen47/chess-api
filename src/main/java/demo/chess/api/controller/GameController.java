package demo.chess.api.controller;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
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

import demo.chess.api.dto.GameAnnotationsRequestDto;
import demo.chess.api.dto.GameSettingsDto;
import demo.chess.api.dto.GameSnapshotDto;
import demo.chess.api.dto.UciGameDto;
import demo.chess.api.service.AnalysisReplayService;
import demo.chess.api.service.ChessDatabaseService;
import demo.chess.api.service.GameLifecycleService;
import demo.chess.api.service.GameService;
import demo.chess.api.service.UciGameService;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.load.SinglePgnGameReader;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService gameService;
    private final GameLifecycleService gameLifecycleService;
    private final UciGameService uciGameService;
    private final AnalysisReplayService analysisReplayService;
    private final ChessDatabaseService chessDatabaseService;
    private final SinglePgnGameReader singlePgnGameReader = new SinglePgnGameReader();

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
     * Returns the current game snapshot used to rehydrate the frontend after a reload.
     *
     * @return current frontend game snapshot
     */
    @GetMapping(value = "/game/state", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getCurrentGameState() {
        try {
            GameSnapshotDto snapshot = uciGameService.getCurrentGameSnapshot();
            return ResponseEntity.ok(snapshot);
        } catch (NoMoveFoundException | IOException e) {
            return ResponseEntity.internalServerError().body("Could not reconstruct current game state");
        }
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
     * Replaces the persistable annotation state of the current game.
     */
    @PostMapping(
            value = "/game/annotations",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> saveGameAnnotations(@RequestBody GameAnnotationsRequestDto request) {
        try {
            String pgn = uciGameService.updateAnnotations(
                    request == null ? null : request.annotations(),
                    request != null && request.whiteComputer(),
                    request != null && request.blackComputer());

            Long gameId = uciGameService.getImportedDatabaseGameId();
            if (gameId == null) {
                gameId = chessDatabaseService.importSingleGameAndResolveId(pgn);
                uciGameService.setImportedDatabaseGameId(gameId);
            }
            chessDatabaseService.saveAnnotatedPgn(gameId, pgn);
            return ResponseEntity.ok(uciGameService.getAnnotationDtos());
        } catch (NoMoveFoundException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (SQLException | IOException e) {
            return ResponseEntity.internalServerError().body("Could not save game annotations: " + e.getMessage());
        }
    }

    /**
     * Imports exactly one PGN game for analysis and stores it in the local database.
     *
     * <p>The request body is consumed as a stream. Reading stops immediately when
     * the beginning of a second game is detected, so a large database PGN is not
     * materialized in the JVM heap before it can be rejected.</p>
     *
     * <p>When exactly one game is present, its original character content is
     * forwarded unchanged to the database and analysis import paths.</p>
     *
     * @param request HTTP request whose body contains the PGN source
     * @return imported analysis game or a structured validation error
     */
    @PostMapping(
            value = "/game/pgn",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importPgnGame(HttpServletRequest request) {
        try {
            return importPgnGame(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("I/O error while reading PGN game");
        }
    }

    /**
     * String-based entry point retained for direct service/controller tests.
     * Production HTTP imports use the streaming servlet request method above.
     *
     * @param content complete PGN content
     * @return imported analysis game or a structured validation error
     */
    public ResponseEntity<?> importPgnGame(String content) {
        return importPgnGame(new StringReader(content == null ? "" : content));
    }

    private ResponseEntity<?> importPgnGame(Reader reader) {
        SinglePgnGameReader.Result probe;
        try {
            probe = singlePgnGameReader.read(reader);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("I/O error while reading PGN game");
        }

        if (probe.gameCount() == 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "PGN_NO_GAME",
                    "gameCount", 0));
        }
        if (probe.gameCount() > 1) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "PGN_MULTIPLE_GAMES",
                    "gameCount", probe.gameCount(),
                    "earlyAbort", probe.earlyAbort()));
        }

        String content = probe.content();
        analysisReplayService.cancel();

        try {
            long gameId = chessDatabaseService.importSingleGameAndResolveId(content);
            UciGameDto importedGame = uciGameService.importGame(content, gameId);
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
