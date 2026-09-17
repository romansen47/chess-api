package demo.chess.api.service;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import demo.chess.api.engine.NativeEngineAvailabilityReason;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.UciEngineInspector;

/**
 * Performs the operating-system and UCI-level health check for one native
 * engine executable.
 *
 * <p>This component intentionally knows nothing about engine roles, profile
 * priorities, browser fallbacks or deep-analysis policy. Those decisions
 * belong to higher-level services.</p>
 */
final class NativeEngineProbe {

    /** Returns a stable cache key for an engine path where possible. */
    String key(String enginePath) {
        try {
            return Path.of(enginePath).toAbsolutePath().normalize().toString();
        } catch (InvalidPathException | NullPointerException e) {
            return enginePath;
        }
    }

    /**
     * Checks file existence, executability and a real UCI handshake.
     *
     * @param config native UCI configuration
     * @return availability reason
     */
    NativeEngineAvailabilityReason probe(UciEngineConfig config) {
        if (config == null || config.getEngine() == null || config.getEngine().isBlank()) {
            return NativeEngineAvailabilityReason.EXECUTABLE_NOT_FOUND;
        }

        Path executable;
        try {
            executable = Path.of(config.getEngine()).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            return NativeEngineAvailabilityReason.EXECUTABLE_NOT_FOUND;
        }
        if (!Files.isRegularFile(executable)) return NativeEngineAvailabilityReason.EXECUTABLE_NOT_FOUND;
        if (!Files.isExecutable(executable)) return NativeEngineAvailabilityReason.NOT_EXECUTABLE;
        try {
            UciEngineInspector.inspect(executable.toString());
            return NativeEngineAvailabilityReason.AVAILABLE;
        } catch (Exception e) {
            return NativeEngineAvailabilityReason.UCI_UNRESPONSIVE;
        }
    }
}
