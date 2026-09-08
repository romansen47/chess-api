package demo.chess.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.AnalysisVariationRequestDto;
import demo.chess.api.service.AnalysisEvaluationService;

@RestController
@RequestMapping("/api/analysis-eval")
public class AnalysisEvaluationController {

    private final AnalysisEvaluationService analysisEvaluationService;

    /**
     * Creates a new AnalysisEvaluationController instance.
     * @param analysisEvaluationService the analysis evaluation service
     */
    public AnalysisEvaluationController(AnalysisEvaluationService analysisEvaluationService) {
        this.analysisEvaluationService = analysisEvaluationService;
    }

    /**
     * Returns the evaluation for one original-game ply.
     * @param ply the ply
     * @return the evaluation
     */
    @GetMapping
    public ResponseEntity<?> getEvaluation(@RequestParam int ply) {
        try {
            return ResponseEntity.ok(analysisEvaluationService.getEvaluation(ply));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Analysis evaluation failed: " + e.getMessage());
        }
    }

    /**
     * Returns the evaluation for a temporary stateless variation.
     * @param request anchor ply plus variation moves
     * @return the evaluation
     */
    @PostMapping("/variation")
    public ResponseEntity<?> getVariationEvaluation(@RequestBody AnalysisVariationRequestDto request) {
        try {
            return ResponseEntity.ok(analysisEvaluationService.getVariationEvaluation(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Analysis variation evaluation failed: " + e.getMessage());
        }
    }

    /**
     * Stops the evaluation.
     * @return the result of the operation
     */
    @PostMapping("/stop")
    public ResponseEntity<?> stopEvaluation() {
        analysisEvaluationService.stopEvaluation();
        return ResponseEntity.ok().build();
    }
}
