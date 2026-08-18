package demo.chess.api.dto;

import java.util.ArrayList;
import java.util.List;

public class EngineConfigOverviewDto {

    private List<EngineDefinitionDto> engines = new ArrayList<>();
    private List<EngineProfileDto> profiles = new ArrayList<>();
    private String evaluationConfigId;
    private String defaultPlayerConfigId;
    private String defaultDeepAnalysisConfigId;
    private long version;

    public EngineConfigOverviewDto() {
    }

    public EngineConfigOverviewDto(
            List<EngineDefinitionDto> engines,
            List<EngineProfileDto> profiles,
            String evaluationConfigId,
            String defaultPlayerConfigId,
            String defaultDeepAnalysisConfigId,
            long version) {
        this.engines = engines != null ? new ArrayList<>(engines) : new ArrayList<>();
        this.profiles = profiles != null ? new ArrayList<>(profiles) : new ArrayList<>();
        this.evaluationConfigId = evaluationConfigId;
        this.defaultPlayerConfigId = defaultPlayerConfigId;
        this.defaultDeepAnalysisConfigId = defaultDeepAnalysisConfigId;
        this.version = version;
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

    public String getEvaluationConfigId() {
        return evaluationConfigId;
    }

    public void setEvaluationConfigId(String evaluationConfigId) {
        this.evaluationConfigId = evaluationConfigId;
    }

    public String getDefaultPlayerConfigId() {
        return defaultPlayerConfigId;
    }

    public void setDefaultPlayerConfigId(String defaultPlayerConfigId) {
        this.defaultPlayerConfigId = defaultPlayerConfigId;
    }

    public String getDefaultDeepAnalysisConfigId() {
        return defaultDeepAnalysisConfigId;
    }

    public void setDefaultDeepAnalysisConfigId(String defaultDeepAnalysisConfigId) {
        this.defaultDeepAnalysisConfigId = defaultDeepAnalysisConfigId;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
