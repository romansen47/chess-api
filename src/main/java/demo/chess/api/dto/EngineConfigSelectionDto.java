package demo.chess.api.dto;

public class EngineConfigSelectionDto {

    private String configId;

    public EngineConfigSelectionDto() {
    }

    public EngineConfigSelectionDto(String configId) {
        this.configId = configId;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }
}
