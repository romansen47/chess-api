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

    public AnalysisReplayController(AnalysisReplayService analysisReplayService) {
        this.analysisReplayService = analysisReplayService;
    }

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

    @GetMapping("/state")
    public ResponseEntity<?> state() {
        return ResponseEntity.ok(analysisReplayService.state());
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancel() {
        return ResponseEntity.ok(analysisReplayService.cancel());
    }
}
