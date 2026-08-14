package demo.chess.api.dto;

import java.util.ArrayList;
import java.util.List;

public class EngineConfigOverviewDto {

    private List<ManagedEngineConfigDto> configs = new ArrayList<>();
    private String evaluationConfigId;
    private String defaultPlayerConfigId;
    private long version;

    public EngineConfigOverviewDto() {
    }

    public EngineConfigOverviewDto(
            List<ManagedEngineConfigDto> configs,
            String evaluationConfigId,
            String defaultPlayerConfigId,
            long version) {
        this.configs = configs != null ? new ArrayList<>(configs) : new ArrayList<>();
        this.evaluationConfigId = evaluationConfigId;
        this.defaultPlayerConfigId = defaultPlayerConfigId;
        this.version = version;
    }

    public List<ManagedEngineConfigDto> getConfigs() {
        return configs;
    }

    public void setConfigs(List<ManagedEngineConfigDto> configs) {
        this.configs = configs != null ? new ArrayList<>(configs) : new ArrayList<>();
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

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
