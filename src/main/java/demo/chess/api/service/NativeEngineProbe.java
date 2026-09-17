package demo.chess.api.service;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import demo.chess.api.engine.NativeEngineAvailabilityReason;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.UciEngineDefinition;
import demo.chess.definitions.engines.UciEngineInspector;

/**
 * Performs operating-system, UCI-health and variant-capability checks for one
 * native engine executable.
 *
 * <p>This component intentionally knows nothing about engine roles, profile
 * priorities, browser fallbacks or deep-analysis policy. A successful UCI
 * handshake proves general UCI availability; a Chess960 request additionally
 * requires the executable to advertise {@code UCI_Chess960}.</p>
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

    /** Checks one engine for classical-chess availability. */
    NativeEngineAvailabilityReason probe(UciEngineConfig config) {
        return probe(config, ChessStartingPosition.STANDARD);
    }

    /**
     * Checks file existence, executability, a real UCI handshake and, when
     * required by the selected game, advertised Chess960 support.
     *
     * @param config native UCI configuration
     * @param startingPosition selected game starting position
     * @return availability reason
     */
    NativeEngineAvailabilityReason probe(
            UciEngineConfig config,
            ChessStartingPosition startingPosition) {
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
            UciEngineDefinition definition = UciEngineInspector.inspect(executable.toString());
            if (requiresChess960(startingPosition) && !definition.supportsChess960()) {
                return NativeEngineAvailabilityReason.CHESS960_UNSUPPORTED;
            }
            return NativeEngineAvailabilityReason.AVAILABLE;
        } catch (Exception e) {
            return NativeEngineAvailabilityReason.UCI_UNRESPONSIVE;
        }
    }

    private boolean requiresChess960(ChessStartingPosition startingPosition) {
        return startingPosition != null && !startingPosition.isStandard();
    }
}
