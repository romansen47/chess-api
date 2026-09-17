package demo.chess.api.service;

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

/**
 * Resolves native-engine capability for API features.
 *
 * <p>The service owns role and fallback policy, while {@link NativeEngineProbe}
 * owns executable/OS/UCI health checks. Deep analysis is intentionally native
 * only: browser Stockfish may keep live evaluation available, but it never
 * satisfies the deep-analysis capability.</p>
 */
@Service
public class EngineAvailabilityService {

    private final EngineRuntimeSelectionService engineRuntimeSelectionService;
    private final NativeEngineProbe engineProbe;
    private final DeepAnalysisProfileResolver deepAnalysisProfileResolver;

    public EngineAvailabilityService(
            EngineRuntimeSelectionService engineRuntimeSelectionService,
            EngineSettingsService engineSettingsService) {
        this.engineRuntimeSelectionService = engineRuntimeSelectionService;
        this.engineProbe = new NativeEngineProbe();
        this.deepAnalysisProfileResolver = new DeepAnalysisProfileResolver(
                engineSettingsService,
                engineProbe);
    }

    /** Returns the current availability of one native-engine role. */
    public NativeEngineAvailability getAvailability(NativeEngineRole role) {
        if (role == null) throw new IllegalArgumentException("Native engine role must not be null");
        if (role == NativeEngineRole.DEEP_ANALYSIS) return getDeepAnalysisAvailability();
        Optional<UciEngineConfig> config = findConfig(role);
        if (config.isEmpty()) return NativeEngineAvailability.notConfigured(role);
        return availability(role, engineProbe.probe(config.get()));
    }

    /**
     * Returns all role capabilities, reusing probe results when multiple roles
     * point at the same executable.
     */
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
            String probeKey = engineProbe.key(resolvedConfig.getEngine());
            NativeEngineAvailabilityReason reason = probeResults.computeIfAbsent(
                    probeKey, ignored -> engineProbe.probe(resolvedConfig));
            result.put(role, availability(role, reason));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Resolves a usable native deep-analysis profile.
     *
     * <p>The requested profile has priority, followed by the configured default
     * and then every other configured native profile. Browser engines are not
     * candidates for deep analysis.</p>
     *
     * @param requestedProfileId explicitly requested profile, or {@code null}
     * @return usable native profile id, when one exists
     */
    public Optional<String> findAvailableDeepAnalysisProfileId(String requestedProfileId) {
        DeepAnalysisProfileResolver.Resolution resolution =
                deepAnalysisProfileResolver.resolve(requestedProfileId);
        return Optional.ofNullable(resolution.availableProfileId());
    }

    /** Returns whether any configured native profile can currently run deep analysis. */
    public NativeEngineAvailability getDeepAnalysisAvailability() {
        DeepAnalysisProfileResolver.Resolution resolution = deepAnalysisProfileResolver.resolve(null);
        if (resolution.available()) {
            return NativeEngineAvailability.available(NativeEngineRole.DEEP_ANALYSIS);
        }
        if (!resolution.configured()) {
            return NativeEngineAvailability.notConfigured(NativeEngineRole.DEEP_ANALYSIS);
        }
        return NativeEngineAvailability.unavailable(
                NativeEngineRole.DEEP_ANALYSIS,
                resolution.reason());
    }

    private Optional<UciEngineConfig> findConfig(NativeEngineRole role) {
        return switch (role) {
            case WHITE_PLAYER -> engineRuntimeSelectionService.findWhitePlayerConfig();
            case BLACK_PLAYER -> engineRuntimeSelectionService.findBlackPlayerConfig();
            case EVALUATION -> engineRuntimeSelectionService.findEvaluationConfig();
            case DEEP_ANALYSIS -> Optional.empty();
        };
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
