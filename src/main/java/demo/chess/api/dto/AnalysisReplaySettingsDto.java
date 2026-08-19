package demo.chess.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Settings of one deep-analysis run. The selected profile only contains UCI
 * values; depth/move-time are use-case limits and therefore live here.
 */
public class AnalysisReplaySettingsDto {

    private String engineProfileId;
    private int depth;
    private int moveTimeSeconds = 5;

    public AnalysisReplaySettingsDto() {
    }

    public AnalysisReplaySettingsDto(String engineProfileId, int depth, int moveTimeSeconds) {
        this.engineProfileId = engineProfileId;
        this.depth = depth;
        this.moveTimeSeconds = moveTimeSeconds;
    }

    public String getEngineProfileId() {
        return engineProfileId;
    }

    public void setEngineProfileId(String engineProfileId) {
        this.engineProfileId = engineProfileId;
    }

    // Backward-compatible request alias for the former property name.
    @JsonProperty("engineConfigId")
    public void setLegacyEngineConfigId(String engineConfigId) {
        if (engineProfileId == null || engineProfileId.isBlank()) {
            this.engineProfileId = engineConfigId;
        }
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = Math.max(0, depth);
    }

    public int getMoveTimeSeconds() {
        return moveTimeSeconds;
    }

    public void setMoveTimeSeconds(int moveTimeSeconds) {
        this.moveTimeSeconds = Math.max(1, moveTimeSeconds);
    }
}
