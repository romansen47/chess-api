package demo.chess.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.EngineConfigOverviewDto;
import demo.chess.api.dto.EngineDefinitionDto;
import demo.chess.api.dto.EngineDefinitionInspectRequestDto;
import demo.chess.api.dto.EngineProfileAssignmentsDto;
import demo.chess.api.dto.EngineProfileDto;
import demo.chess.api.service.EngineSettingsService;

@RestController
@RequestMapping("/api/engine-configs")
public class EngineSettingsController {

    private final EngineSettingsService engineSettingsService;

    public EngineSettingsController(EngineSettingsService engineSettingsService) {
        this.engineSettingsService = engineSettingsService;
    }

    @GetMapping
    public EngineConfigOverviewDto getOverview() {
        return engineSettingsService.getOverview();
    }

    @PostMapping("/discover")
    public ResponseEntity<EngineConfigOverviewDto> discoverSystemEngines() {
        return ResponseEntity.ok(engineSettingsService.discoverSystemEngines());
    }

    @PostMapping("/reset")
    public ResponseEntity<EngineConfigOverviewDto> resetEngineSettings() {
        return ResponseEntity.ok(engineSettingsService.resetToFallbackDefaults());
    }

    @PutMapping("/defaults")
    public ResponseEntity<EngineConfigOverviewDto> updateDefaults(
            @RequestBody EngineProfileAssignmentsDto assignments) {
        engineSettingsService.updateDefaultAssignments(assignments);
        return ResponseEntity.ok(engineSettingsService.getOverview());
    }

    @PostMapping("/engines/inspect")
    public ResponseEntity<EngineDefinitionDto> inspectEngine(
            @RequestBody EngineDefinitionInspectRequestDto request) {
        return ResponseEntity.ok(engineSettingsService.inspectEngineDefinition(
                request.getEngine(),
                request.getName()));
    }

    @PostMapping("/engines")
    public ResponseEntity<EngineDefinitionDto> createEngine(
            @RequestBody EngineDefinitionDto engine) {
        return ResponseEntity.ok(engineSettingsService.createEngine(engine));
    }

    @PutMapping("/engines/{id}")
    public ResponseEntity<EngineDefinitionDto> updateEngine(
            @PathVariable String id,
            @RequestBody EngineDefinitionDto engine) {
        return ResponseEntity.ok(engineSettingsService.updateEngine(id, engine));
    }

    @DeleteMapping("/engines/{id}")
    public ResponseEntity<Void> deleteEngine(@PathVariable String id) {
        engineSettingsService.deleteEngine(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/profiles")
    public ResponseEntity<EngineProfileDto> createProfile(
            @RequestBody EngineProfileDto profile) {
        return ResponseEntity.ok(engineSettingsService.createProfile(profile));
    }

    @PutMapping("/profiles/{id}")
    public ResponseEntity<EngineProfileDto> updateProfile(
            @PathVariable String id,
            @RequestBody EngineProfileDto profile) {
        return ResponseEntity.ok(engineSettingsService.updateProfile(id, profile));
    }

    @DeleteMapping("/profiles/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String id) {
        engineSettingsService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }
}
