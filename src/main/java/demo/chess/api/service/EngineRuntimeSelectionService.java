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

    /** @return effective White-player config when one is available */
    public synchronized Optional<UciEngineConfig> findWhitePlayerConfig() {
        return findConfig(getEffectiveWhitePlayerProfileId());
    }

    /** @return effective Black-player config when one is available */
    public synchronized Optional<UciEngineConfig> findBlackPlayerConfig() {
        return findConfig(getEffectiveBlackPlayerProfileId());
    }

    /** @return effective live-evaluation config when one is available */
    public synchronized Optional<UciEngineConfig> findEvaluationConfig() {
        return findConfig(getEffectiveEvaluationProfileId());
    }

    /** @return effective White-player config
     * @throws NativeEngineUnavailableException when none is configured
     */
    public synchronized UciEngineConfig requireWhitePlayerConfig() {
        return findWhitePlayerConfig()
                .orElseThrow(() -> new NativeEngineUnavailableException(NativeEngineRole.WHITE_PLAYER));
    }

    /** @return effective Black-player config
     * @throws NativeEngineUnavailableException when none is configured
     */
    public synchronized UciEngineConfig requireBlackPlayerConfig() {
        return findBlackPlayerConfig()
                .orElseThrow(() -> new NativeEngineUnavailableException(NativeEngineRole.BLACK_PLAYER));
    }

    /** @return effective live-evaluation config
     * @throws NativeEngineUnavailableException when none is configured
     */
    public synchronized UciEngineConfig requireEvaluationConfig() {
        return findEvaluationConfig()
                .orElseThrow(() -> new NativeEngineUnavailableException(NativeEngineRole.EVALUATION));
    }

    /** @return effective White-player config */
    public synchronized UciEngineConfig getWhitePlayerConfig() {
        return requireWhitePlayerConfig();
    }

    /** @return effective Black-player config */
    public synchronized UciEngineConfig getBlackPlayerConfig() {
        return requireBlackPlayerConfig();
    }

    /** @return effective live-evaluation config */
    public synchronized UciEngineConfig getEvaluationConfig() {
        return requireEvaluationConfig();
    }

    /** @return White-player engine path when configured */
    public synchronized Optional<String> findWhitePlayerEnginePath() {
        return findWhitePlayerConfig().map(UciEngineConfig::getEngine);
    }

    /** @return Black-player engine path when configured */
    public synchronized Optional<String> findBlackPlayerEnginePath() {
        return findBlackPlayerConfig().map(UciEngineConfig::getEngine);
    }

    /** @return live-evaluation engine path when configured */
    public synchronized Optional<String> findEvaluationEnginePath() {
        return findEvaluationConfig().map(UciEngineConfig::getEngine);
    }

    /** @return required White-player engine path */
    public synchronized String getWhitePlayerEnginePath() {
        return requireWhitePlayerConfig().getEngine();
    }

    /** @return required Black-player engine path */
    public synchronized String getBlackPlayerEnginePath() {
        return requireBlackPlayerConfig().getEngine();
    }

    /** @return required live-evaluation engine path */
    public synchronized String getEvaluationEnginePath() {
        return requireEvaluationConfig().getEngine();
    }

    /** @return display name of the effective White-player engine */
    public synchronized String getWhitePlayerEngineName() {
        return engineSettingsService.getEngineName(getWhitePlayerEnginePath());
    }

    /** @return display name of the effective Black-player engine */
    public synchronized String getBlackPlayerEngineName() {
        return engineSettingsService.getEngineName(getBlackPlayerEnginePath());
    }

    /** @return display name of the effective live-evaluation engine */
    public synchronized String getEvaluationEngineName() {
        return engineSettingsService.getEngineName(getEvaluationEnginePath());
    }

    /** @return version token for White-player runtime selection */
    public synchronized long getWhitePlayerVersion() {
        return combinedVersion(whitePlayerSelectionVersion);
    }

    /** @return version token for Black-player runtime selection */
    public synchronized long getBlackPlayerVersion() {
        return combinedVersion(blackPlayerSelectionVersion);
    }

    /** @return version token for live-evaluation runtime selection */
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
