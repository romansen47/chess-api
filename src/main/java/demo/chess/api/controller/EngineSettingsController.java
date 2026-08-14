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

import demo.chess.api.dto.EngineConfigInspectRequestDto;
import demo.chess.api.dto.EngineConfigOverviewDto;
import demo.chess.api.dto.EngineConfigSelectionDto;
import demo.chess.api.dto.ManagedEngineConfigDto;
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

    @PostMapping("/inspect")
    public ResponseEntity<ManagedEngineConfigDto> inspectEngine(
            @RequestBody EngineConfigInspectRequestDto request) {
        return ResponseEntity.ok(engineSettingsService.inspectEngine(
                request.getEngine(),
                request.getName(),
                request.getType()));
    }

    @PostMapping
    public ResponseEntity<ManagedEngineConfigDto> createConfig(
            @RequestBody ManagedEngineConfigDto config) {
        return ResponseEntity.ok(engineSettingsService.createConfig(config));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ManagedEngineConfigDto> updateConfig(
            @PathVariable String id,
            @RequestBody ManagedEngineConfigDto config) {
        return ResponseEntity.ok(engineSettingsService.updateConfig(id, config));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable String id) {
        engineSettingsService.deleteConfig(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/evaluation")
    public ResponseEntity<EngineConfigOverviewDto> selectEvaluationConfig(
            @RequestBody EngineConfigSelectionDto selection) {
        engineSettingsService.setEvaluationConfigId(selection.getConfigId());
        return ResponseEntity.ok(engineSettingsService.getOverview());
    }
}
