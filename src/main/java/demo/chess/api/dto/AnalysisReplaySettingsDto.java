package demo.chess.api.dto;

public class AnalysisReplaySettingsDto {

    private String engineConfigId;

    public AnalysisReplaySettingsDto() {
    }

    public AnalysisReplaySettingsDto(String engineConfigId) {
        this.engineConfigId = engineConfigId;
    }

    public String getEngineConfigId() {
        return engineConfigId;
    }

    public void setEngineConfigId(String engineConfigId) {
        this.engineConfigId = engineConfigId;
    }
}
