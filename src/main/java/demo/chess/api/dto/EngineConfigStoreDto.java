package demo.chess.api.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class EngineConfigStoreDto {

    private List<EngineDefinitionDto> engines = new ArrayList<>();
    private List<EngineProfileDto> profiles = new ArrayList<>();
    private EngineProfileAssignmentsDto defaults = new EngineProfileAssignmentsDto();
    private String fallbackProfileId;

    // Legacy combined model. Read-only for migration and never written again.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<ManagedEngineConfigDto> configs = new ArrayList<>();

    // Legacy assignment fields from the typed-profile implementation.
    @JsonProperty(value = "defaultPlayerConfigId", access = JsonProperty.Access.WRITE_ONLY)
    private String legacyDefaultPlayerConfigId;
    @JsonProperty(value = "defaultEvaluationConfigId", access = JsonProperty.Access.WRITE_ONLY)
    private String legacyDefaultEvaluationConfigId;
    @JsonProperty(value = "defaultDeepAnalysisConfigId", access = JsonProperty.Access.WRITE_ONLY)
    private String legacyDefaultDeepAnalysisConfigId;
    @JsonProperty(value = "evaluationConfigId", access = JsonProperty.Access.WRITE_ONLY)
    private String legacyEvaluationConfigId;

    /**
     * Creates a new EngineConfigStoreDto instance.
     */
    public EngineConfigStoreDto() {
    }

    /**
     * Returns the engines.
     * @return the engines
     */
    public List<EngineDefinitionDto> getEngines() {
        return engines;
    }

    /**
     * Sets the engines.
     * @param engines the engines
     */
    public void setEngines(List<EngineDefinitionDto> engines) {
        this.engines = engines != null ? new ArrayList<>(engines) : new ArrayList<>();
    }

    /**
     * Returns the profiles.
     * @return the profiles
     */
    public List<EngineProfileDto> getProfiles() {
        return profiles;
    }

    /**
     * Sets the profiles.
     * @param profiles the profiles
     */
    public void setProfiles(List<EngineProfileDto> profiles) {
        this.profiles = profiles != null ? new ArrayList<>(profiles) : new ArrayList<>();
    }

    /**
     * Returns the defaults.
     * @return the defaults
     */
    public EngineProfileAssignmentsDto getDefaults() {
        if (defaults == null) {
            defaults = new EngineProfileAssignmentsDto();
        }
        return defaults;
    }

    /**
     * Sets the defaults.
     * @param defaults the defaults
     */
    public void setDefaults(EngineProfileAssignmentsDto defaults) {
        this.defaults = defaults != null ? defaults : new EngineProfileAssignmentsDto();
    }

    /**
     * Returns the fallback profile id.
     * @return the fallback profile id
     */
    public String getFallbackProfileId() {
        return fallbackProfileId;
    }

    /**
     * Sets the fallback profile id.
     * @param fallbackProfileId the fallback profile id
     */
    public void setFallbackProfileId(String fallbackProfileId) {
        this.fallbackProfileId = fallbackProfileId;
    }

    /**
     * Returns the legacy configs.
     * @return the legacy configs
     */
    @JsonIgnore
    public List<ManagedEngineConfigDto> getLegacyConfigs() {
        return configs;
    }

    /**
     * Returns the legacy default player config id.
     * @return the legacy default player config id
     */
    @JsonIgnore
    public String getLegacyDefaultPlayerConfigId() {
        return legacyDefaultPlayerConfigId;
    }

    /**
     * Returns the legacy default evaluation config id.
     * @return the legacy default evaluation config id
     */
    @JsonIgnore
    public String getLegacyDefaultEvaluationConfigId() {
        return legacyDefaultEvaluationConfigId;
    }

    /**
     * Returns the legacy default deep analysis config id.
     * @return the legacy default deep analysis config id
     */
    @JsonIgnore
    public String getLegacyDefaultDeepAnalysisConfigId() {
        return legacyDefaultDeepAnalysisConfigId;
    }

    /**
     * Returns the legacy evaluation config id.
     * @return the legacy evaluation config id
     */
    @JsonIgnore
    public String getLegacyEvaluationConfigId() {
        return legacyEvaluationConfigId;
    }
}
