package demo.chess.api.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.EngineRuntimeAssignmentsDto;
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

    public EngineRuntimeSelectionService(EngineSettingsService engineSettingsService) {
        this.engineSettingsService = engineSettingsService;
    }

    public synchronized EngineRuntimeAssignmentsDto getAssignments() {
        return new EngineRuntimeAssignmentsDto(
                validOverride(whitePlayerProfileId),
                validOverride(blackPlayerProfileId),
                validOverride(evaluationProfileId));
    }

    public synchronized void setWhitePlayerProfileId(String profileId) {
        String normalized = normalizeOverride(profileId);
        if (!Objects.equals(normalized, whitePlayerProfileId)) {
            whitePlayerProfileId = normalized;
            whitePlayerSelectionVersion++;
        }
    }

    public synchronized void setBlackPlayerProfileId(String profileId) {
        String normalized = normalizeOverride(profileId);
        if (!Objects.equals(normalized, blackPlayerProfileId)) {
            blackPlayerProfileId = normalized;
            blackPlayerSelectionVersion++;
        }
    }

    public synchronized void setEvaluationProfileId(String profileId) {
        String normalized = normalizeOverride(profileId);
        if (!Objects.equals(normalized, evaluationProfileId)) {
            evaluationProfileId = normalized;
            evaluationSelectionVersion++;
        }
    }

    public synchronized String getEffectiveWhitePlayerProfileId() {
        String override = validOverride(whitePlayerProfileId);
        return override != null ? override : engineSettingsService.getDefaultWhitePlayerProfileId();
    }

    public synchronized String getEffectiveBlackPlayerProfileId() {
        String override = validOverride(blackPlayerProfileId);
        return override != null ? override : engineSettingsService.getDefaultBlackPlayerProfileId();
    }

    public synchronized String getEffectiveEvaluationProfileId() {
        String override = validOverride(evaluationProfileId);
        return override != null ? override : engineSettingsService.getDefaultEvaluationProfileId();
    }

    public synchronized UciEngineConfig getWhitePlayerConfig() {
        return engineSettingsService.getConfig(getEffectiveWhitePlayerProfileId());
    }

    public synchronized UciEngineConfig getBlackPlayerConfig() {
        return engineSettingsService.getConfig(getEffectiveBlackPlayerProfileId());
    }

    public synchronized UciEngineConfig getEvaluationConfig() {
        return engineSettingsService.getConfig(getEffectiveEvaluationProfileId());
    }

    public synchronized String getWhitePlayerEnginePath() {
        return getWhitePlayerConfig().getEngine();
    }

    public synchronized String getBlackPlayerEnginePath() {
        return getBlackPlayerConfig().getEngine();
    }

    public synchronized String getEvaluationEnginePath() {
        return getEvaluationConfig().getEngine();
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
