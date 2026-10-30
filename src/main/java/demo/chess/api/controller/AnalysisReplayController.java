package demo.chess.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.AnalysisReplaySettingsDto;
import demo.chess.api.dto.AnalysisReplayStepDto;
import demo.chess.api.exception.NativeEngineUnavailableException;
import demo.chess.api.service.AnalysisReplayService;
import demo.chess.definitions.engines.management.UciEngineLogEntry;

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
     * Returns the current replay state without advancing the analysis.
     *
     * @return current replay state, or 204 when no replay session exists
     */
    @GetMapping("/state")
    public ResponseEntity<?> state() {
        AnalysisReplayStepDto state = analysisReplayService.state();
        return state == null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(state);
    }

    /** Returns raw UCI traffic while Deep Analysis is running. */
    @GetMapping("/engine-log")
    public List<UciEngineLogEntry> engineLog() {
        return analysisReplayService.activeEngineLog();
    }

    /**
     * Starts a new deep-analysis replay with the requested engine settings.
     *
     * @param settings analysis settings, or {@code null} for service defaults
     * @return initial replay state
     */
    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestBody(required = false) AnalysisReplaySettingsDto settings) {
        try {
            return ResponseEntity.ok(analysisReplayService.start(settings));
        } catch (NativeEngineUnavailableException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Analysis replay could not be started: " + e.getMessage());
        }
    }

    /**
     * Advances the active deep-analysis replay by one ply.
     *
     * @return updated replay state
     */
    @PostMapping("/next")
    public ResponseEntity<?> next() {
        try {
            return ResponseEntity.ok(analysisReplayService.next());
        } catch (NativeEngineUnavailableException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Analysis replay step failed: " + e.getMessage());
        }
    }

    /**
     * Cancels the active deep-analysis replay and stops its engine.
     *
     * @return final cancelled replay state
     */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancel() {
        return ResponseEntity.ok(analysisReplayService.cancel());
    }
}
