package demo.chess.api.dto;

import java.util.ArrayList;
import java.util.List;

public class EngineConfigStoreDto {

    private List<ManagedEngineConfigDto> configs = new ArrayList<>();
    private String defaultPlayerConfigId;
    private String defaultEvaluationConfigId;
    private String evaluationConfigId;

    public EngineConfigStoreDto() {
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

    public String getEvaluationConfigId() {
        return evaluationConfigId;
    }

    public void setEvaluationConfigId(String evaluationConfigId) {
        this.evaluationConfigId = evaluationConfigId;
    }
}
