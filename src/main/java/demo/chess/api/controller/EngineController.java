package demo.chess.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import demo.chess.api.dto.EngineEvaluationDto;
import demo.chess.api.service.EvaluationService;
import demo.chess.api.service.LiveEvaluationStreamService;

@RestController
@RequestMapping("/api")
public class EngineController {

    private final EvaluationService evaluationService;
    private final LiveEvaluationStreamService liveEvaluationStreamService;

    /**
     * Creates a new EngineController instance.
     * @param evaluationService the evaluation service
     * @param liveEvaluationStreamService SSE stream publisher
     */
    public EngineController(
            EvaluationService evaluationService,
            LiveEvaluationStreamService liveEvaluationStreamService) {
        this.evaluationService = evaluationService;
        this.liveEvaluationStreamService = liveEvaluationStreamService;
    }

    /**
     * Returns the evaluation.
     * @return the evaluation
     */
    @GetMapping("/eval")
    public ResponseEntity<?> getEvaluation() {
        try {
            EngineEvaluationDto dto = evaluationService.getEvaluation();
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            System.err.println("[EngineController] Error while evaluating position: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Engine error: " + e.getMessage());
        }
    }

    /**
     * Opens the server-sent-event stream for live-evaluation bar updates.
     * Evaluation itself continues to be started and synchronized by the normal
     * GET /api/eval lifecycle used for the engine-lines display.
     * @return SSE emitter
     */
    @GetMapping(path = "/eval/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvaluation() {
        return liveEvaluationStreamService.subscribe();
    }

    /**
     * Stops the evaluation.
     * @return the result of the operation
     */
    @PostMapping("/eval/stop")
    public ResponseEntity<?> stopEvaluation() {
        evaluationService.stopLiveEvaluation();
        return ResponseEntity.ok().build();
    }
}
