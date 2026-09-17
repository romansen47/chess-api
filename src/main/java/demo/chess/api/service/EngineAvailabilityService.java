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
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.engines.UciEngineConfig;

/**
 * Resolves native-engine capability for API features.
 *
 * <p>The service owns role and fallback policy, while {@link NativeEngineProbe}
 * owns executable/OS/UCI health and variant-capability checks. Deep analysis is
 * intentionally native only: browser Stockfish may keep live evaluation
 * available, but it never satisfies the deep-analysis capability.</p>
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

    /** Returns one native-engine role capability for classical chess. */
    public NativeEngineAvailability getAvailability(NativeEngineRole role) {
        return getAvailability(role, ChessStartingPosition.STANDARD);
    }

    /**
     * Returns the current availability of one native-engine role for the given
     * starting position.
     *
     * @param role native-engine role
     * @param startingPosition game/analysis starting position
     * @return variant-aware capability
     */
    public NativeEngineAvailability getAvailability(
            NativeEngineRole role,
            ChessStartingPosition startingPosition) {
        if (role == null) throw new IllegalArgumentException("Native engine role must not be null");
        ChessStartingPosition resolvedPosition = resolveStartingPosition(startingPosition);
        if (role == NativeEngineRole.DEEP_ANALYSIS) {
            return getDeepAnalysisAvailability(resolvedPosition);
        }
        Optional<UciEngineConfig> config = findConfig(role);
        if (config.isEmpty()) return NativeEngineAvailability.notConfigured(role);
        return availability(role, engineProbe.probe(config.get(), resolvedPosition));
    }

    /** Returns all role capabilities for classical chess. */
    public Map<NativeEngineRole, NativeEngineAvailability> getAvailabilities() {
        return getAvailabilities(ChessStartingPosition.STANDARD);
    }

    /**
     * Returns all role capabilities for one starting position, reusing probe
     * results when multiple roles point at the same executable.
     */
    public Map<NativeEngineRole, NativeEngineAvailability> getAvailabilities(
            ChessStartingPosition startingPosition) {
        ChessStartingPosition resolvedPosition = resolveStartingPosition(startingPosition);
        EnumMap<NativeEngineRole, NativeEngineAvailability> result = new EnumMap<>(NativeEngineRole.class);
        Map<String, NativeEngineAvailabilityReason> probeResults = new HashMap<>();
        for (NativeEngineRole role : NativeEngineRole.values()) {
            if (role == NativeEngineRole.DEEP_ANALYSIS) {
                result.put(role, getDeepAnalysisAvailability(resolvedPosition));
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
                    probeKey,
                    ignored -> engineProbe.probe(resolvedConfig, resolvedPosition));
            result.put(role, availability(role, reason));
        }
        return Collections.unmodifiableMap(result);
    }

    /** Resolves a usable native deep-analysis profile for classical chess. */
    public Optional<String> findAvailableDeepAnalysisProfileId(String requestedProfileId) {
        return findAvailableDeepAnalysisProfileId(
                requestedProfileId,
                ChessStartingPosition.STANDARD);
    }

    /**
     * Resolves a usable native deep-analysis profile for the selected variant.
     *
     * <p>The requested profile has priority, followed by the configured default
     * and then every other configured native profile. A profile that speaks UCI
     * but lacks {@code UCI_Chess960} is skipped for a non-standard starting
     * position. Browser engines are not candidates for deep analysis.</p>
     *
     * @param requestedProfileId explicitly requested profile, or {@code null}
     * @param startingPosition selected analysis starting position
     * @return usable native profile id, when one exists
     */
    public Optional<String> findAvailableDeepAnalysisProfileId(
            String requestedProfileId,
            ChessStartingPosition startingPosition) {
        DeepAnalysisProfileResolver.Resolution resolution =
                deepAnalysisProfileResolver.resolve(
                        requestedProfileId,
                        resolveStartingPosition(startingPosition));
        return Optional.ofNullable(resolution.availableProfileId());
    }

    /** Returns whether any configured native profile can run classical deep analysis. */
    public NativeEngineAvailability getDeepAnalysisAvailability() {
        return getDeepAnalysisAvailability(ChessStartingPosition.STANDARD);
    }

    /**
     * Returns whether any configured native profile can run deep analysis for
     * the selected starting position.
     */
    public NativeEngineAvailability getDeepAnalysisAvailability(
            ChessStartingPosition startingPosition) {
        DeepAnalysisProfileResolver.Resolution resolution = deepAnalysisProfileResolver.resolve(
                null,
                resolveStartingPosition(startingPosition));
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

    private ChessStartingPosition resolveStartingPosition(ChessStartingPosition startingPosition) {
        return startingPosition != null ? startingPosition : ChessStartingPosition.STANDARD;
    }
}
