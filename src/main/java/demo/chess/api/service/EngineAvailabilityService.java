package demo.chess.api.service;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

import org.springframework.stereotype.Service;

import demo.chess.api.engine.NativeEngineAvailability;
import demo.chess.api.engine.NativeEngineAvailabilityReason;
import demo.chess.api.engine.NativeEngineRole;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.UciEngineInspector;

/**
 * Determines whether the native engine assigned to one application role is
 * currently usable.
 *
 * <p>Configuration ownership remains with {@link EngineSettingsService} and
 * {@link EngineRuntimeSelectionService}. This service only resolves the
 * effective configuration and probes the configured executable.</p>
 */
@Service
public class EngineAvailabilityService {

    private final EngineRuntimeSelectionService engineRuntimeSelectionService;
    private final EngineSettingsService engineSettingsService;

    public EngineAvailabilityService(
            EngineRuntimeSelectionService engineRuntimeSelectionService,
            EngineSettingsService engineSettingsService) {
        this.engineRuntimeSelectionService = engineRuntimeSelectionService;
        this.engineSettingsService = engineSettingsService;
    }

    /**
     * Returns the current availability of one native-engine role.
     * @param role the role
     * @return availability state
     */
    public NativeEngineAvailability getAvailability(NativeEngineRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Native engine role must not be null");
        }

        Optional<UciEngineConfig> config = findConfig(role);
        if (config.isEmpty()) {
            return NativeEngineAvailability.notConfigured(role);
        }
        return probe(role, config.get());
    }

    private Optional<UciEngineConfig> findConfig(NativeEngineRole role) {
        return switch (role) {
            case WHITE_PLAYER -> engineRuntimeSelectionService.findWhitePlayerConfig();
            case BLACK_PLAYER -> engineRuntimeSelectionService.findBlackPlayerConfig();
            case EVALUATION -> engineRuntimeSelectionService.findEvaluationConfig();
            case DEEP_ANALYSIS -> findDeepAnalysisConfig();
        };
    }

    private Optional<UciEngineConfig> findDeepAnalysisConfig() {
        String profileId = engineSettingsService.getDefaultDeepAnalysisProfileId();
        if (profileId == null || profileId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(engineSettingsService.getConfig(profileId));
    }

    private NativeEngineAvailability probe(
            NativeEngineRole role,
            UciEngineConfig config) {
        Path executable;
        try {
            executable = Path.of(config.getEngine()).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            return NativeEngineAvailability.unavailable(
                    role,
                    NativeEngineAvailabilityReason.EXECUTABLE_NOT_FOUND);
        }

        if (!Files.isRegularFile(executable)) {
            return NativeEngineAvailability.unavailable(
                    role,
                    NativeEngineAvailabilityReason.EXECUTABLE_NOT_FOUND);
        }

        if (!Files.isExecutable(executable)) {
            return NativeEngineAvailability.unavailable(
                    role,
                    NativeEngineAvailabilityReason.NOT_EXECUTABLE);
        }

        try {
            UciEngineInspector.inspect(executable.toString());
            return NativeEngineAvailability.available(role);
        } catch (Exception e) {
            return NativeEngineAvailability.unavailable(
                    role,
                    NativeEngineAvailabilityReason.UCI_UNRESPONSIVE);
        }
    }
}
