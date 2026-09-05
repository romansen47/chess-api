package demo.chess.api.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import demo.chess.api.dto.ChessDatabaseDtos;
import demo.chess.api.dto.UciGameDto;
import demo.chess.api.service.AnalysisReplayService;
import demo.chess.api.service.ChessDatabaseService;
import demo.chess.definitions.engines.impl.NoMoveFoundException;

/**
 * REST endpoints for the embedded local chess database.
 */
@RestController
@RequestMapping("/api/chess-database")
public class ChessDatabaseController {

    private final ChessDatabaseService chessDatabaseService;
    private final AnalysisReplayService analysisReplayService;

    /**
     * Creates a new database controller.
     *
     * @param chessDatabaseService database service
     * @param analysisReplayService analysis replay service
     */
    public ChessDatabaseController(
            ChessDatabaseService chessDatabaseService,
            AnalysisReplayService analysisReplayService) {
        this.chessDatabaseService = chessDatabaseService;
        this.analysisReplayService = analysisReplayService;
    }

    /**
     * Returns database availability and summary information.
     *
     * @return database status
     */
    @GetMapping("/status")
    public ResponseEntity<ChessDatabaseDtos.Status> getStatus() {
        return ResponseEntity.ok(chessDatabaseService.getStatus());
    }

    /**
     * Imports one PGN file into the embedded database.
     *
     * @param file uploaded PGN file
     * @return completed import result
     */
    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importPgn(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Choose a non-empty PGN file.");
        }

        try {
            return ResponseEntity.ok(chessDatabaseService.importPgn(file.getInputStream()));
        } catch (SQLException | IOException e) {
            return ResponseEntity.internalServerError().body("Chess database import failed: " + e.getMessage());
        }
    }

    /**
     * Searches the local database.
     *
     * @param request search request
     * @return matching games
     */
    @PostMapping(
            value = "/search",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> search(@RequestBody(required = false) ChessDatabaseDtos.SearchRequest request) {
        try {
            List<ChessDatabaseDtos.GameSummary> result = chessDatabaseService.search(request);
            return ResponseEntity.ok(result);
        } catch (SQLException | IOException e) {
            return ResponseEntity.internalServerError().body("Chess database search failed: " + e.getMessage());
        }
    }

    /**
     * Loads a stored game into the existing PGN analysis workflow.
     *
     * @param gameId database game identifier
     * @return imported game
     */
    @PostMapping(
            value = "/games/{gameId}/load",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> loadGame(@PathVariable long gameId) {
        analysisReplayService.cancel();

        try {
            UciGameDto game = chessDatabaseService.loadGame(gameId);
            return ResponseEntity.ok(game);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (NoMoveFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SQLException | IOException e) {
            return ResponseEntity.internalServerError().body("Could not load database game: " + e.getMessage());
        }
    }

    /**
     * Returns move statistics for the currently selected analysis position.
     *
     * @param ply selected ply
     * @return position statistics
     */
    @GetMapping(
            value = "/position",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPosition(@RequestParam(name = "ply", defaultValue = "0") int ply) {
        try {
            return ResponseEntity.ok(chessDatabaseService.getPositionStatistics(ply));
        } catch (NoMoveFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SQLException | IOException e) {
            return ResponseEntity.internalServerError().body("Could not query position database: " + e.getMessage());
        }
    }
}
