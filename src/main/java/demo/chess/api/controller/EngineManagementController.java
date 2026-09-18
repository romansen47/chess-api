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

    /**
     * Performs the list processes operation.
     * @return the result of the operation
     */
    @GetMapping
    public List<UciEngineProcessInfo> listProcesses() {
        return UciEngineProcessManager.list();
    }

    /**
     * Returns the process log.
     * @param id the id
     * @return the process log
     */
    @GetMapping("/{id}/log")
    public List<UciEngineLogEntry> getProcessLog(@PathVariable String id) {
        return UciEngineProcessManager.log(id);
    }

    /** Stops an engine through its owning UCI adapter. */
    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, Object>> stop(@PathVariable String id) {
        boolean found = UciEngineProcessManager.stop(id);
        if (!found) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("stopped", true, "id", id));
    }

    /** Emergency process kill that bypasses the owning UCI adapter. */
    @PostMapping("/{id}/force-terminate")
    public ResponseEntity<Map<String, Object>> forceTerminate(@PathVariable String id) {
        boolean found = UciEngineProcessManager.forceTerminate(id);
        if (!found) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("terminated", true, "forced", true, "id", id));
    }

    /** Backward-compatible alias for the graceful stop path. */
    @PostMapping("/{id}/terminate")
    public ResponseEntity<Map<String, Object>> terminate(@PathVariable String id) {
        return stop(id);
    }
}
