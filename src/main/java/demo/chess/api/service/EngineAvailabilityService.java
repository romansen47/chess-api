package demo.chess.api.service;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.EngineProfileDto;
import demo.chess.api.engine.NativeEngineAvailability;
import demo.chess.api.engine.NativeEngineAvailabilityReason;
import demo.chess.api.engine.NativeEngineRole;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.UciEngineInspector;

/** Determines whether native engines are currently usable. */
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

    public NativeEngineAvailability getAvailability(NativeEngineRole role) {
        if (role == null) throw new IllegalArgumentException("Native engine role must not be null");
        if (role == NativeEngineRole.DEEP_ANALYSIS) return getDeepAnalysisAvailability();
        Optional<UciEngineConfig> config = findConfig(role);
        if (config.isEmpty()) return NativeEngineAvailability.notConfigured(role);
        return availability(role, probe(config.get()));
    }

    public Map<NativeEngineRole, NativeEngineAvailability> getAvailabilities() {
        EnumMap<NativeEngineRole, NativeEngineAvailability> result = new EnumMap<>(NativeEngineRole.class);
        Map<String, NativeEngineAvailabilityReason> probeResults = new HashMap<>();
        for (NativeEngineRole role : NativeEngineRole.values()) {
            if (role == NativeEngineRole.DEEP_ANALYSIS) {
                result.put(role, getDeepAnalysisAvailability());
                continue;
            }
            Optional<UciEngineConfig> config = findConfig(role);
            if (config.isEmpty()) {
                result.put(role, NativeEngineAvailability.notConfigured(role));
                continue;
            }
            UciEngineConfig resolvedConfig = config.get();
            String probeKey = probeKey(resolvedConfig.getEngine());
            NativeEngineAvailabilityReason reason = probeResults.computeIfAbsent(
                    probeKey, ignored -> probe(resolvedConfig));
            result.put(role, availability(role, reason));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Resolves a usable native deep-analysis profile. Requested/default profile
     * wins; other configured native profiles are tried as fallbacks. Browser
     * engines are intentionally outside this service.
     */
    public Optional<String> findAvailableDeepAnalysisProfileId(String requestedProfileId) {
        Set<String> candidates = new LinkedHashSet<>();
        if (requestedProfileId != null && !requestedProfileId.isBlank()) candidates.add(requestedProfileId.trim());
        String defaultId = engineSettingsService.getDefaultDeepAnalysisProfileId();
        if (defaultId != null && !defaultId.isBlank()) candidates.add(defaultId);
        for (EngineProfileDto profile : engineSettingsService.getOverview().getProfiles()) {
            if (profile != null && profile.getId() != null && !profile.getId().isBlank()) candidates.add(profile.getId());
        }

        for (String profileId : candidates) {
            try {
                UciEngineConfig config = engineSettingsService.getConfig(profileId);
                if (probe(config) == NativeEngineAvailabilityReason.AVAILABLE) return Optional.of(profileId);
            } catch (RuntimeException ignored) {
                // Stale/missing profile; continue with another native profile.
            }
        }
        return Optional.empty();
    }

    public NativeEngineAvailability getDeepAnalysisAvailability() {
        boolean configured = !engineSettingsService.getOverview().getProfiles().isEmpty();
        Optional<String> availableProfile = findAvailableDeepAnalysisProfileId(
                engineSettingsService.getDefaultDeepAnalysisProfileId());
        if (availableProfile.isPresent()) return NativeEngineAvailability.available(NativeEngineRole.DEEP_ANALYSIS);
        if (!configured) return NativeEngineAvailability.notConfigured(NativeEngineRole.DEEP_ANALYSIS);

        String defaultId = engineSettingsService.getDefaultDeepAnalysisProfileId();
        if (defaultId != null && !defaultId.isBlank()) {
            try {
                return availability(NativeEngineRole.DEEP_ANALYSIS, probe(engineSettingsService.getConfig(defaultId)));
            } catch (RuntimeException ignored) {
            }
        }
        return NativeEngineAvailability.unavailable(
                NativeEngineRole.DEEP_ANALYSIS,
                NativeEngineAvailabilityReason.UCI_UNRESPONSIVE);
    }

    private Optional<UciEngineConfig> findConfig(NativeEngineRole role) {
        return switch (role) {
            case WHITE_PLAYER -> engineRuntimeSelectionService.findWhitePlayerConfig();
            case BLACK_PLAYER -> engineRuntimeSelectionService.findBlackPlayerConfig();
            case EVALUATION -> engineRuntimeSelectionService.findEvaluationConfig();
            case DEEP_ANALYSIS -> Optional.empty();
        };
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
        if (!Files.isRegularFile(executable)) return NativeEngineAvailabilityReason.EXECUTABLE_NOT_FOUND;
        if (!Files.isExecutable(executable)) return NativeEngineAvailabilityReason.NOT_EXECUTABLE;
        try {
            UciEngineInspector.inspect(executable.toString());
            return NativeEngineAvailabilityReason.AVAILABLE;
        } catch (Exception e) {
            return NativeEngineAvailabilityReason.UCI_UNRESPONSIVE;
        }
    }

    private NativeEngineAvailability availability(NativeEngineRole role, NativeEngineAvailabilityReason reason) {
        if (reason == NativeEngineAvailabilityReason.AVAILABLE) return NativeEngineAvailability.available(role);
        return NativeEngineAvailability.unavailable(role, reason);
    }
}
