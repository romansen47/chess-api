package demo.chess.api.service;

import java.util.LinkedHashSet;
import java.util.Set;

import demo.chess.api.dto.EngineProfileDto;
import demo.chess.api.engine.NativeEngineAvailabilityReason;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.engines.UciEngineConfig;

/**
 * Applies the native deep-analysis profile fallback policy.
 *
 * <p>Resolution order is deterministic: an explicitly requested profile,
 * then the configured deep-analysis default, then every remaining configured
 * native profile. Browser engines are deliberately not part of this resolver.
 * The first profile that passes {@link NativeEngineProbe} for the selected
 * starting position wins.</p>
 */
final class DeepAnalysisProfileResolver {

    private final EngineSettingsService engineSettingsService;
    private final NativeEngineProbe engineProbe;

    DeepAnalysisProfileResolver(
            EngineSettingsService engineSettingsService,
            NativeEngineProbe engineProbe) {
        this.engineSettingsService = engineSettingsService;
        this.engineProbe = engineProbe;
    }

    /** Resolves a native profile for classical chess. */
    Resolution resolve(String requestedProfileId) {
        return resolve(requestedProfileId, ChessStartingPosition.STANDARD);
    }

    /**
     * Resolves the best currently usable native profile for the selected game
     * variant.
     *
     * @param requestedProfileId explicitly requested profile, or {@code null}
     * @param startingPosition selected analysis starting position
     * @return resolution result
     */
    Resolution resolve(
            String requestedProfileId,
            ChessStartingPosition startingPosition) {
        Set<String> candidates = new LinkedHashSet<>();
        addCandidate(candidates, requestedProfileId);
        addCandidate(candidates, engineSettingsService.getDefaultDeepAnalysisProfileId());
        for (EngineProfileDto profile : engineSettingsService.getOverview().getProfiles()) {
            if (profile != null) addCandidate(candidates, profile.getId());
        }

        boolean configured = !engineSettingsService.getOverview().getProfiles().isEmpty();
        NativeEngineAvailabilityReason firstFailure = null;
        for (String profileId : candidates) {
            try {
                UciEngineConfig config = engineSettingsService.getConfig(profileId);
                NativeEngineAvailabilityReason reason = engineProbe.probe(config, startingPosition);
                if (reason == NativeEngineAvailabilityReason.AVAILABLE) {
                    return new Resolution(profileId, configured, NativeEngineAvailabilityReason.AVAILABLE);
                }
                if (firstFailure == null) firstFailure = reason;
            } catch (RuntimeException e) {
                if (firstFailure == null) firstFailure = NativeEngineAvailabilityReason.UCI_UNRESPONSIVE;
            }
        }

        NativeEngineAvailabilityReason failure = firstFailure != null
                ? firstFailure
                : NativeEngineAvailabilityReason.UCI_UNRESPONSIVE;
        return new Resolution(null, configured, failure);
    }

    private void addCandidate(Set<String> candidates, String profileId) {
        if (profileId != null && !profileId.isBlank()) candidates.add(profileId.trim());
    }

    /** Immutable result of one fallback-resolution pass. */
    record Resolution(
            String availableProfileId,
            boolean configured,
            NativeEngineAvailabilityReason reason) {

        boolean available() {
            return availableProfileId != null;
        }
    }
}
