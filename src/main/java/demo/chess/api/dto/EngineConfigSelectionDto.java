package demo.chess.api.dto;

public class EngineConfigSelectionDto {

    private String configId;

    /**
     * Creates a new EngineConfigSelectionDto instance.
     */
    public EngineConfigSelectionDto() {
    }

    /**
     * Creates a new EngineConfigSelectionDto instance.
     * @param configId the config id
     */
    public EngineConfigSelectionDto(String configId) {
        this.configId = configId;
    }

    /**
     * Returns the config id.
     * @return the config id
     */
    public String getConfigId() {
        return configId;
    }

    /**
     * Sets the config id.
     * @param configId the config id
     */
    public void setConfigId(String configId) {
        this.configId = configId;
    }
}
