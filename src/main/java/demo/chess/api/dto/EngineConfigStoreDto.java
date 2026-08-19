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

    public EngineConfigStoreDto() {
    }

    public List<EngineDefinitionDto> getEngines() {
        return engines;
    }

    public void setEngines(List<EngineDefinitionDto> engines) {
        this.engines = engines != null ? new ArrayList<>(engines) : new ArrayList<>();
    }

    public List<EngineProfileDto> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<EngineProfileDto> profiles) {
        this.profiles = profiles != null ? new ArrayList<>(profiles) : new ArrayList<>();
    }

    public EngineProfileAssignmentsDto getDefaults() {
        if (defaults == null) {
            defaults = new EngineProfileAssignmentsDto();
        }
        return defaults;
    }

    public void setDefaults(EngineProfileAssignmentsDto defaults) {
        this.defaults = defaults != null ? defaults : new EngineProfileAssignmentsDto();
    }

    public String getFallbackProfileId() {
        return fallbackProfileId;
    }

    public void setFallbackProfileId(String fallbackProfileId) {
        this.fallbackProfileId = fallbackProfileId;
    }

    @JsonIgnore
    public List<ManagedEngineConfigDto> getLegacyConfigs() {
        return configs;
    }

    @JsonIgnore
    public String getLegacyDefaultPlayerConfigId() {
        return legacyDefaultPlayerConfigId;
    }

    @JsonIgnore
    public String getLegacyDefaultEvaluationConfigId() {
        return legacyDefaultEvaluationConfigId;
    }

    @JsonIgnore
    public String getLegacyDefaultDeepAnalysisConfigId() {
        return legacyDefaultDeepAnalysisConfigId;
    }

    @JsonIgnore
    public String getLegacyEvaluationConfigId() {
        return legacyEvaluationConfigId;
    }
}
