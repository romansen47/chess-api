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

    /**
     * Imports a UCI engine definition from the executable itself.
     *
     * UCI option defaults are capabilities reported by the engine and are not
     * user configuration. Therefore the client supplied option map is ignored
     * here and the executable is inspected again before the definition is
     * persisted. Concrete option values belong to EngineProfileDto.
     */
    @PostMapping("/engines")
    public ResponseEntity<EngineDefinitionDto> createEngine(
            @RequestBody EngineDefinitionDto engine) {
        if (engine == null) {
            throw new IllegalArgumentException("Engine definition must not be null");
        }

        EngineDefinitionDto inspected = engineSettingsService.inspectEngineDefinition(
                engine.getEngine(),
                engine.getName());
        return ResponseEntity.ok(engineSettingsService.createEngine(inspected));
    }

    /**
     * Existing engine definitions keep the UCI metadata/options that were read
     * from the binary when the engine was imported. Only the user-facing name
     * may be changed. Engine parameters are configured exclusively in profiles.
     */
    @PutMapping("/engines/{id}")
    public ResponseEntity<EngineDefinitionDto> updateEngine(
            @PathVariable String id,
            @RequestBody EngineDefinitionDto engine) {
        if (engine == null) {
            throw new IllegalArgumentException("Engine definition must not be null");
        }

        EngineDefinitionDto existing = engineSettingsService.getOverview().getEngines().stream()
                .filter(candidate -> id.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown engine id: " + id));

        EngineDefinitionDto safeUpdate = new EngineDefinitionDto();
        safeUpdate.setId(existing.getId());
        safeUpdate.setName(engine.getName());
        safeUpdate.setEngine(existing.getEngine());
        safeUpdate.setEngineName(existing.getEngineName());
        safeUpdate.setEngineAuthor(existing.getEngineAuthor());
        safeUpdate.setOptions(existing.getOptions());

        return ResponseEntity.ok(engineSettingsService.updateEngine(id, safeUpdate));
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
