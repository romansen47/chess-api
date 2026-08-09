package demo.chess.api.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.definitions.engines.management.UciEngineLogEntry;
import demo.chess.definitions.engines.management.UciEngineProcessInfo;
import demo.chess.definitions.engines.management.UciEngineProcessManager;

@RestController
@RequestMapping("/api/engine-processes")
public class EngineManagementController {

    @GetMapping
    public List<UciEngineProcessInfo> listProcesses() {
        return UciEngineProcessManager.list();
    }

    @GetMapping("/{id}/log")
    public List<UciEngineLogEntry> getProcessLog(@PathVariable String id) {
        return UciEngineProcessManager.log(id);
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<Map<String, Object>> terminate(@PathVariable String id) {
        boolean found = UciEngineProcessManager.terminate(id);
        if (!found) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("terminated", true, "id", id));
    }
}
