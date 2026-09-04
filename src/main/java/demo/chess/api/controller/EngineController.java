package demo.chess.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.EngineEvaluationDto;
import demo.chess.api.service.EvaluationService;

@RestController
@RequestMapping("/api")
public class EngineController {

    private final EvaluationService evaluationService;

    /**
     * Creates a new EngineController instance.
     * @param evaluationService the evaluation service
     */
    public EngineController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
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
     * Stops the evaluation.
     * @return the result of the operation
     */
    @PostMapping("/eval/stop")
    public ResponseEntity<?> stopEvaluation() {
        evaluationService.stopLiveEvaluation();
        return ResponseEntity.ok().build();
    }
}
