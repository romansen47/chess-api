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
import demo.chess.api.service.NativeEngineFilePickerService;

@RestController
@RequestMapping("/api/engine-configs")
public class EngineSettingsController {

    private final EngineSettingsService engineSettingsService;
    private final NativeEngineFilePickerService nativeEngineFilePickerService;

    /**
     * Creates a new EngineSettingsController instance.
     * @param engineSettingsService the engine settings service
     * @param nativeEngineFilePickerService the native engine file picker service
     */
    public EngineSettingsController(
            EngineSettingsService engineSettingsService,
            NativeEngineFilePickerService nativeEngineFilePickerService) {
        this.engineSettingsService = engineSettingsService;
        this.nativeEngineFilePickerService = nativeEngineFilePickerService;
    }

    /**
     * Returns the overview.
     * @return the overview
     */
    @GetMapping
    public EngineConfigOverviewDto getOverview() {
        return engineSettingsService.getOverview();
    }

    /**
     * Performs the discover system engines operation.
     * @return the result of the operation
     */
    @PostMapping("/discover")
    public ResponseEntity<EngineConfigOverviewDto> discoverSystemEngines() {
        return ResponseEntity.ok(engineSettingsService.discoverSystemEngines());
    }

    /**
     * Resets the engine settings.
     * @return the result of the operation
     */
    @PostMapping("/reset")
    public ResponseEntity<EngineConfigOverviewDto> resetEngineSettings() {
        return ResponseEntity.ok(engineSettingsService.resetToFallbackDefaults());
    }

    /**
     * Updates the defaults.
     * @param assignments the assignments
     * @return the result of the operation
     */
    @PutMapping("/defaults")
    public ResponseEntity<EngineConfigOverviewDto> updateDefaults(
            @RequestBody EngineProfileAssignmentsDto assignments) {
        engineSettingsService.updateDefaultAssignments(assignments);
        return ResponseEntity.ok(engineSettingsService.getOverview());
    }

    /**
     * Performs the select engine operation.
     * @return the result of the operation
     */
    @PostMapping("/engines/select")
    public ResponseEntity<EngineDefinitionDto> selectEngine() {
        String enginePath = nativeEngineFilePickerService.selectExecutable();
        if (enginePath == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(engineSettingsService.inspectEngineDefinition(enginePath, null));
    }

    /**
     * Performs the inspect engine operation.
     * @param request the request
     * @return the result of the operation
     */
    @PostMapping("/engines/inspect")
    public ResponseEntity<EngineDefinitionDto> inspectEngine(
            @RequestBody EngineDefinitionInspectRequestDto request) {
        return ResponseEntity.ok(engineSettingsService.inspectEngineDefinition(
                request.getEngine(),
                request.getName()));
    }

    /**
     * Creates the engine.
     * @param engine the engine
     * @return the result of the operation
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
     * Updates the engine.
     * @param id the id
     * @param engine the engine
     * @return the result of the operation
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

    /**
     * Deletes the engine.
     * @param id the id
     * @return the result of the operation
     */
    @DeleteMapping("/engines/{id}")
    public ResponseEntity<Void> deleteEngine(@PathVariable String id) {
        engineSettingsService.deleteEngine(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates the profile.
     * @param profile the profile
     * @return the result of the operation
     */
    @PostMapping("/profiles")
    public ResponseEntity<EngineProfileDto> createProfile(
            @RequestBody EngineProfileDto profile) {
        return ResponseEntity.ok(engineSettingsService.createProfile(profile));
    }

    /**
     * Updates the profile.
     * @param id the id
     * @param profile the profile
     * @return the result of the operation
     */
    @PutMapping("/profiles/{id}")
    public ResponseEntity<EngineProfileDto> updateProfile(
            @PathVariable String id,
            @RequestBody EngineProfileDto profile) {
        return ResponseEntity.ok(engineSettingsService.updateProfile(id, profile));
    }

    /**
     * Deletes the profile.
     * @param id the id
     * @return the result of the operation
     */
    @DeleteMapping("/profiles/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String id) {
        engineSettingsService.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }
}
