package demo.chess.api.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EngineConfigStoreDto {

    private List<EngineDefinitionDto> engines = new ArrayList<>();
    private List<EngineProfileDto> profiles = new ArrayList<>();

    // Legacy field. It is read once to migrate the former combined config model
    // and is no longer written by the new service.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private List<ManagedEngineConfigDto> configs = new ArrayList<>();

    private String defaultPlayerConfigId;
    private String defaultEvaluationConfigId;
    private String defaultDeepAnalysisConfigId;
    private String evaluationConfigId;

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

    public List<ManagedEngineConfigDto> getConfigs() {
        return configs;
    }

    public void setConfigs(List<ManagedEngineConfigDto> configs) {
        this.configs = configs != null ? new ArrayList<>(configs) : new ArrayList<>();
    }

    public String getDefaultPlayerConfigId() {
        return defaultPlayerConfigId;
    }

    public void setDefaultPlayerConfigId(String defaultPlayerConfigId) {
        this.defaultPlayerConfigId = defaultPlayerConfigId;
    }

    public String getDefaultEvaluationConfigId() {
        return defaultEvaluationConfigId;
    }

    public void setDefaultEvaluationConfigId(String defaultEvaluationConfigId) {
        this.defaultEvaluationConfigId = defaultEvaluationConfigId;
    }

    public String getDefaultDeepAnalysisConfigId() {
        return defaultDeepAnalysisConfigId;
    }

    public void setDefaultDeepAnalysisConfigId(String defaultDeepAnalysisConfigId) {
        this.defaultDeepAnalysisConfigId = defaultDeepAnalysisConfigId;
    }

    public String getEvaluationConfigId() {
        return evaluationConfigId;
    }

    public void setEvaluationConfigId(String evaluationConfigId) {
        this.evaluationConfigId = evaluationConfigId;
    }
}
