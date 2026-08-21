package demo.chess.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.service.AnalysisEvaluationService;

@RestController
@RequestMapping("/api/analysis-eval")
public class AnalysisEvaluationController {

    private final AnalysisEvaluationService analysisEvaluationService;

    public AnalysisEvaluationController(AnalysisEvaluationService analysisEvaluationService) {
        this.analysisEvaluationService = analysisEvaluationService;
    }

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

    @PostMapping("/stop")
    public ResponseEntity<?> stopEvaluation() {
        analysisEvaluationService.stopEvaluation();
        return ResponseEntity.ok().build();
    }
}
