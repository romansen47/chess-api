package demo.chess.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.GameAnalysisDtos;
import demo.chess.api.service.GameAnalysisHistoryService;

/** REST boundary for immutable game-analysis history and comparisons. */
@RestController
@RequestMapping("/api/game-analyses")
public class GameAnalysisController {

    private final GameAnalysisHistoryService historyService;

    public GameAnalysisController(GameAnalysisHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        try {
            List<GameAnalysisDtos.RunSummary> runs = historyService.listCurrentGameRuns();
            return ResponseEntity.ok(runs);
        } catch (Exception e) {
            return failure(e);
        }
    }

    @GetMapping("/{runId}")
    public ResponseEntity<?> get(@PathVariable long runId) {
        try {
            return ResponseEntity.ok(historyService.getRun(runId));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @PostMapping("/{runId}/active")
    public ResponseEntity<?> activate(@PathVariable long runId) {
        try {
            historyService.activate(runId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return failure(e);
        }
    }

    @GetMapping("/compare")
    public ResponseEntity<?> compare(
            @RequestParam long leftRunId,
            @RequestParam long rightRunId) {
        try {
            return ResponseEntity.ok(historyService.compare(leftRunId, rightRunId));
        } catch (Exception e) {
            return failure(e);
        }
    }

    private ResponseEntity<String> failure(Exception exception) {
        HttpStatus status = exception instanceof IllegalArgumentException
                ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(exception.getMessage());
    }
}
