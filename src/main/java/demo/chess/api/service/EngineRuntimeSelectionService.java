package demo.chess.api.service;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.EngineRuntimeAssignmentsDto;
import demo.chess.api.engine.NativeEngineRole;
import demo.chess.api.exception.NativeEngineUnavailableException;
import demo.chess.definitions.engines.UciEngineConfig;

/**
 * Holds temporary, non-persistent engine profile overrides for the current
 * application runtime.
 *
 * <p>Persistent defaults remain owned by {@link EngineSettingsService}. A
 * {@code null} override means that the corresponding persistent default is
 * used. Runtime selections deliberately survive new games but disappear when
 * the application is restarted.</p>
 */
@Service
public class EngineRuntimeSelectionService {

    private final EngineSettingsService engineSettingsService;

    private String whitePlayerProfileId;
    private String blackPlayerProfileId;
    private String evaluationProfileId;

    private long whitePlayerSelectionVersion = 1L;
    private long blackPlayerSelectionVersion = 1L;
    private long evaluationSelectionVersion = 1L;

    /**
     * Creates the runtime-selection overlay above persistent engine settings.
     *
     * @param engineSettingsService persistent engine/profile settings
     */
    public EngineRuntimeSelectionService(EngineSettingsService engineSettingsService) {
        this.engineSettingsService = engineSettingsService;
    }

    /**
     * Returns the currently valid runtime overrides.
     *
     * @return runtime assignment snapshot
     */
    public synchronized EngineRuntimeAssignmentsDto getAssignments() {
        return new EngineRuntimeAssignmentsDto(
                validOverride(whitePlayerProfileId),
                validOverride(blackPlayerProfileId),
                validOverride(evaluationProfileId));
    }

    /**
     * Sets or clears the White-player runtime profile override.
     *
     * @param profileId profile id; blank clears the override
     */
    public synchronized void setWhitePlayerProfileId(String profileId) {
        String normalized = normalizeOverride(profileId);
        if (!Objects.equals(normalized, whitePlayerProfileId)) {
            whitePlayerProfileId = normalized;
            whitePlayerSelectionVersion++;
        }
    }

    /**
     * Sets or clears the Black-player runtime profile override.
     *
     * @param profileId profile id; blank clears the override
     */
    public synchronized void setBlackPlayerProfileId(String profileId) {
        String normalized = normalizeOverride(profileId);
        if (!Objects.equals(normalized, blackPlayerProfileId)) {
            blackPlayerProfileId = normalized;
            blackPlayerSelectionVersion++;
        }
    }

    /**
     * Sets or clears the live-evaluation runtime profile override.
     *
     * @param profileId profile id; blank clears the override
     */
    public synchronized void setEvaluationProfileId(String profileId) {
        String normalized = normalizeOverride(profileId);
        if (!Objects.equals(normalized, evaluationProfileId)) {
            evaluationProfileId = normalized;
            evaluationSelectionVersion++;
        }
    }

    /**
     * Returns the effective White-player profile after applying the runtime override.
     *
     * @return effective profile id, or {@code null}
     */
    public synchronized String getEffectiveWhitePlayerProfileId() {
        String override = validOverride(whitePlayerProfileId);
        return override != null ? override : engineSettingsService.getDefaultWhitePlayerProfileId();
    }

    /**
     * Returns the effective Black-player profile after applying the runtime override.
     *
     * @return effective profile id, or {@code null}
     */
    public synchronized String getEffectiveBlackPlayerProfileId() {
        String override = validOverride(blackPlayerProfileId);
        return override != null ? override : engineSettingsService.getDefaultBlackPlayerProfileId();
    }

    /**
     * Returns the effective live-evaluation profile after applying the runtime override.
     *
     * @return effective profile id, or {@code null}
     */
    public synchronized String getEffectiveEvaluationProfileId() {
        String override = validOverride(evaluationProfileId);
        return override != null ? override : engineSettingsService.getDefaultEvaluationProfileId();
    }

    public synchronized Optional<UciEngineConfig> findWhitePlayerConfig() {
        return findConfig(getEffectiveWhitePlayerProfileId());
    }

    public synchronized Optional<UciEngineConfig> findBlackPlayerConfig() {
        return findConfig(getEffectiveBlackPlayerProfileId());
    }

    public synchronized Optional<UciEngineConfig> findEvaluationConfig() {
        return findConfig(getEffectiveEvaluationProfileId());
    }

    public synchronized UciEngineConfig requireWhitePlayerConfig() {
        return findWhitePlayerConfig()
                .orElseThrow(() -> new NativeEngineUnavailableException(NativeEngineRole.WHITE_PLAYER));
    }

    public synchronized UciEngineConfig requireBlackPlayerConfig() {
        return findBlackPlayerConfig()
                .orElseThrow(() -> new NativeEngineUnavailableException(NativeEngineRole.BLACK_PLAYER));
    }

    public synchronized UciEngineConfig requireEvaluationConfig() {
        return findEvaluationConfig()
                .orElseThrow(() -> new NativeEngineUnavailableException(NativeEngineRole.EVALUATION));
    }

    public synchronized UciEngineConfig getWhitePlayerConfig() {
        return requireWhitePlayerConfig();
    }

    public synchronized UciEngineConfig getBlackPlayerConfig() {
        return requireBlackPlayerConfig();
    }

    public synchronized UciEngineConfig getEvaluationConfig() {
        return requireEvaluationConfig();
    }

    public synchronized Optional<String> findWhitePlayerEnginePath() {
        return findWhitePlayerConfig().map(UciEngineConfig::getEngine);
    }

    public synchronized Optional<String> findBlackPlayerEnginePath() {
        return findBlackPlayerConfig().map(UciEngineConfig::getEngine);
    }

    public synchronized Optional<String> findEvaluationEnginePath() {
        return findEvaluationConfig().map(UciEngineConfig::getEngine);
    }

    public synchronized String getWhitePlayerEnginePath() {
        return requireWhitePlayerConfig().getEngine();
    }

    public synchronized String getBlackPlayerEnginePath() {
        return requireBlackPlayerConfig().getEngine();
    }

    public synchronized String getEvaluationEnginePath() {
        return requireEvaluationConfig().getEngine();
    }

    public synchronized String getWhitePlayerEngineName() {
        return engineSettingsService.getEngineName(getWhitePlayerEnginePath());
    }

    public synchronized String getBlackPlayerEngineName() {
        return engineSettingsService.getEngineName(getBlackPlayerEnginePath());
    }

    public synchronized String getEvaluationEngineName() {
        return engineSettingsService.getEngineName(getEvaluationEnginePath());
    }

    public synchronized long getWhitePlayerVersion() {
        return combinedVersion(whitePlayerSelectionVersion);
    }

    public synchronized long getBlackPlayerVersion() {
        return combinedVersion(blackPlayerSelectionVersion);
    }

    public synchronized long getEvaluationVersion() {
        return combinedVersion(evaluationSelectionVersion);
    }

    private Optional<UciEngineConfig> findConfig(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(engineSettingsService.getConfig(profileId));
    }

    private String normalizeOverride(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            return null;
        }
        String normalized = profileId.trim();
        engineSettingsService.getConfig(normalized);
        return normalized;
    }

    private String validOverride(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            return null;
        }
        try {
            engineSettingsService.getConfig(profileId);
            return profileId;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private long combinedVersion(long selectionVersion) {
        return engineSettingsService.getVersion() * 31L + selectionVersion;
    }
}
