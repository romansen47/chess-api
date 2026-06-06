package demo.chess.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.EngineSettingsDto;
import demo.chess.api.service.EngineSettingsService; 

@RestController
@RequestMapping("/api/stockfish")
public class EngineSettingsController {

    private final EngineSettingsService engineSettingsService;

    public EngineSettingsController(EngineSettingsService engineSettingsService) {
        this.engineSettingsService = engineSettingsService;
    }

    @GetMapping("/settings")
    public EngineSettingsDto getSettings() {
        return engineSettingsService.getSettings();
    }

    @PutMapping("/settings")
    public ResponseEntity<EngineSettingsDto> updateSettings(@RequestBody EngineSettingsDto settings) {
        return ResponseEntity.ok(engineSettingsService.updateSettings(settings));
    }
}
