package demo.chess.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.ClockDto;
import demo.chess.api.service.ClockService;

@RestController
@RequestMapping("/api")
public class ClockController {

    private final ClockService clockService;

    public ClockController(ClockService clockService) {
        this.clockService = clockService;
    }

    @GetMapping("/clock")
    public ResponseEntity<ClockDto> getClock() {
        return ResponseEntity.ok(clockService.getClock());
    }
}