package demo.chess.api.controller;

import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.EngineRuntimeAssignmentsDto;
import demo.chess.api.dto.EngineRuntimeProfileSelectionDto;
import demo.chess.api.service.EngineRuntimeSelectionService;

@RestController
@RequestMapping("/api/engine-configs/runtime")
public class EngineRuntimeController {

    private final EngineRuntimeSelectionService engineRuntimeSelectionService;

    public EngineRuntimeController(EngineRuntimeSelectionService engineRuntimeSelectionService) {
        this.engineRuntimeSelectionService = engineRuntimeSelectionService;
    }

    @GetMapping
    public EngineRuntimeAssignmentsDto getAssignments() {
        return engineRuntimeSelectionService.getAssignments();
    }

    @PutMapping("/{target}")
    public ResponseEntity<?> updateAssignment(
            @PathVariable String target,
            @RequestBody(required = false) EngineRuntimeProfileSelectionDto selection) {
        String profileId = selection != null ? selection.getProfileId() : null;
        String normalizedTarget = target == null ? "" : target.trim().toLowerCase(Locale.ROOT);

        try {
            switch (normalizedTarget) {
                case "white" -> engineRuntimeSelectionService.setWhitePlayerProfileId(profileId);
                case "black" -> engineRuntimeSelectionService.setBlackPlayerProfileId(profileId);
                case "evaluation" -> engineRuntimeSelectionService.setEvaluationProfileId(profileId);
                default -> {
                    return ResponseEntity.badRequest().body("Unsupported runtime engine target: " + target);
                }
            }
            return ResponseEntity.ok(engineRuntimeSelectionService.getAssignments());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
