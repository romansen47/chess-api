package demo.chess.api.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.AnalysisVariationMoveResultDto;
import demo.chess.api.dto.AnalysisVariationRequestDto;
import demo.chess.api.dto.PossibleMovesResponse;
import demo.chess.api.service.AnalysisVariationService;
import demo.chess.definitions.engines.impl.NoMoveFoundException;

/**
 * Stateless API for temporary analysis variations.
 */
@RestController
@RequestMapping("/api/analysis-variation")
public class AnalysisVariationController {

    private final AnalysisVariationService analysisVariationService;

    /**
     * Creates a new AnalysisVariationController instance.
     * @param analysisVariationService the variation service
     */
    public AnalysisVariationController(AnalysisVariationService analysisVariationService) {
        this.analysisVariationService = analysisVariationService;
    }

    /**
     * Returns legal target squares in a reconstructed variation position.
     * @param request variation request
     * @return legal target squares
     */
    @PostMapping("/possible-moves")
    public ResponseEntity<?> getPossibleMoves(@RequestBody AnalysisVariationRequestDto request) {
        try {
            List<String> targets = analysisVariationService.getPossibleTargets(request);
            return ResponseEntity.ok(new PossibleMovesResponse(request.getFrom(), targets));
        } catch (IllegalArgumentException | NoMoveFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Could not reconstruct analysis variation");
        }
    }

    /**
     * Applies one move to a reconstructed variation position.
     * @param request variation request plus requested move
     * @return resulting position
     */
    @PostMapping("/move")
    public ResponseEntity<?> makeMove(@RequestBody AnalysisVariationRequestDto request) {
        try {
            return ResponseEntity.ok(analysisVariationService.applyMove(request));
        } catch (IllegalArgumentException | NoMoveFoundException e) {
            AnalysisVariationMoveResultDto result = new AnalysisVariationMoveResultDto(
                    false,
                    e.getMessage(),
                    request != null ? request.getFrom() : null,
                    request != null ? request.getTo() : null,
                    null,
                    null,
                    null,
                    null);
            return ResponseEntity.badRequest().body(result);
        } catch (IOException e) {
            AnalysisVariationMoveResultDto result = new AnalysisVariationMoveResultDto(
                    false,
                    "I/O error while applying analysis variation move",
                    request != null ? request.getFrom() : null,
                    request != null ? request.getTo() : null,
                    null,
                    null,
                    null,
                    null);
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
