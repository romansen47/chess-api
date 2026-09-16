package demo.chess.api.service;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
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
        return availability(role, probe(config.get()));
    }

    /**
     * Returns the current availability of all native-engine roles.
     *
     * <p>When several roles use the same executable, that executable is probed
     * only once for this snapshot. No result is cached across calls.</p>
     * @return immutable availability map keyed by role
     */
    public Map<NativeEngineRole, NativeEngineAvailability> getAvailabilities() {
        EnumMap<NativeEngineRole, NativeEngineAvailability> result =
                new EnumMap<>(NativeEngineRole.class);
        Map<String, NativeEngineAvailabilityReason> probeResults = new HashMap<>();

        for (NativeEngineRole role : NativeEngineRole.values()) {
            Optional<UciEngineConfig> config = findConfig(role);
            if (config.isEmpty()) {
                result.put(role, NativeEngineAvailability.notConfigured(role));
                continue;
            }

            UciEngineConfig resolvedConfig = config.get();
            String probeKey = probeKey(resolvedConfig.getEngine());
            NativeEngineAvailabilityReason reason = probeResults.computeIfAbsent(
                    probeKey,
                    ignored -> probe(resolvedConfig));
            result.put(role, availability(role, reason));
        }

        return Collections.unmodifiableMap(result);
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

    private String probeKey(String enginePath) {
        try {
            return Path.of(enginePath).toAbsolutePath().normalize().toString();
        } catch (InvalidPathException e) {
            return enginePath;
        }
    }

    private NativeEngineAvailabilityReason probe(UciEngineConfig config) {
        Path executable;
        try {
            executable = Path.of(config.getEngine()).toAbsolutePath().normalize();
        } catch (InvalidPathException e) {
            return NativeEngineAvailabilityReason.EXECUTABLE_NOT_FOUND;
        }

        if (!Files.isRegularFile(executable)) {
            return NativeEngineAvailabilityReason.EXECUTABLE_NOT_FOUND;
        }

        if (!Files.isExecutable(executable)) {
            return NativeEngineAvailabilityReason.NOT_EXECUTABLE;
        }

        try {
            UciEngineInspector.inspect(executable.toString());
            return NativeEngineAvailabilityReason.AVAILABLE;
        } catch (Exception e) {
            return NativeEngineAvailabilityReason.UCI_UNRESPONSIVE;
        }
    }

    private NativeEngineAvailability availability(
            NativeEngineRole role,
            NativeEngineAvailabilityReason reason) {
        if (reason == NativeEngineAvailabilityReason.AVAILABLE) {
            return NativeEngineAvailability.available(role);
        }
        return NativeEngineAvailability.unavailable(role, reason);
    }

}
