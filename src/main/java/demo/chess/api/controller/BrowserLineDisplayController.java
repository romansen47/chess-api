package demo.chess.api.controller;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.BrowserLineDisplayRequestDto;
import demo.chess.api.service.BrowserLineDisplayService;
import demo.chess.definitions.engines.impl.NoMoveFoundException;

/** Stateless display enrichment for locally calculated browser-engine lines. */
@RestController
@RequestMapping("/api/eval/browser-lines")
public class BrowserLineDisplayController {

    private final BrowserLineDisplayService lineDisplayService;

    /**
     * Creates the controller.
     * @param lineDisplayService browser-line presentation service
     */
    public BrowserLineDisplayController(BrowserLineDisplayService lineDisplayService) {
        this.lineDisplayService = lineDisplayService;
    }

    /**
     * Adds SAN, board positions and arrows to raw browser UCI lines.
     * @param request raw browser-engine lines and exact position context
     * @return enriched lines, or an explanatory client/server error
     */
    @PostMapping
    public ResponseEntity<?> enrich(@RequestBody BrowserLineDisplayRequestDto request) {
        try {
            return ResponseEntity.ok(lineDisplayService.enrich(request));
        } catch (IllegalArgumentException | NoMoveFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Could not reconstruct browser engine lines");
        }
    }
}
