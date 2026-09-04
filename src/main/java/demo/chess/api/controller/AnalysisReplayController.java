package demo.chess.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.AnalysisReplaySettingsDto;
import demo.chess.api.service.AnalysisReplayService;

@RestController
@RequestMapping("/api/analysis-replay")
public class AnalysisReplayController {

    private final AnalysisReplayService analysisReplayService;

    /**
     * Creates a new AnalysisReplayController instance.
     * @param analysisReplayService the analysis replay service
     */
    public AnalysisReplayController(AnalysisReplayService analysisReplayService) {
        this.analysisReplayService = analysisReplayService;
    }

    /**
     * Performs the start operation.
     * @param settings the settings
     * @return the result of the operation
     */
    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody(required = false) AnalysisReplaySettingsDto settings) {
        try {
            return ResponseEntity.ok(analysisReplayService.start(settings));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Analysis replay could not be started: " + e.getMessage());
        }
    }

    /**
     * Performs the next operation.
     * @return the result of the operation
     */
    @PostMapping("/next")
    public ResponseEntity<?> next() {
        try {
            return ResponseEntity.ok(analysisReplayService.next());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Analysis replay step failed: " + e.getMessage());
        }
    }

    /**
     * Performs the state operation.
     * @return the result of the operation
     */
    @GetMapping("/state")
    public ResponseEntity<?> state() {
        return ResponseEntity.ok(analysisReplayService.state());
    }

    /**
     * Returns whether this object can cel.
     * @return true when the condition is satisfied; otherwise false
     */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancel() {
        return ResponseEntity.ok(analysisReplayService.cancel());
    }
}
