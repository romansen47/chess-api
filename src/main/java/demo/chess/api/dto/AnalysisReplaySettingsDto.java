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

    /**
     * Creates a new AnalysisReplaySettingsDto instance.
     */
    public AnalysisReplaySettingsDto() {
    }

    /**
     * Creates a new AnalysisReplaySettingsDto instance.
     * @param engineProfileId the engine profile id
     * @param depth the depth
     * @param moveTimeSeconds the move time seconds
     */
    public AnalysisReplaySettingsDto(String engineProfileId, int depth, int moveTimeSeconds) {
        this.engineProfileId = engineProfileId;
        this.depth = depth;
        this.moveTimeSeconds = moveTimeSeconds;
    }

    /**
     * Returns the engine profile id.
     * @return the engine profile id
     */
    public String getEngineProfileId() {
        return engineProfileId;
    }

    /**
     * Sets the engine profile id.
     * @param engineProfileId the engine profile id
     */
    public void setEngineProfileId(String engineProfileId) {
        this.engineProfileId = engineProfileId;
    }

    // Backward-compatible request alias for the former property name.
    /**
     * Sets the legacy engine config id.
     * @param engineConfigId the engine config id
     */
    @JsonProperty("engineConfigId")
    public void setLegacyEngineConfigId(String engineConfigId) {
        if (engineProfileId == null || engineProfileId.isBlank()) {
            this.engineProfileId = engineConfigId;
        }
    }

    /**
     * Returns the depth.
     * @return the depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Sets the depth.
     * @param depth the depth
     */
    public void setDepth(int depth) {
        this.depth = Math.max(0, depth);
    }

    /**
     * Returns the move time seconds.
     * @return the move time seconds
     */
    public int getMoveTimeSeconds() {
        return moveTimeSeconds;
    }

    /**
     * Sets the move time seconds.
     * @param moveTimeSeconds the move time seconds
     */
    public void setMoveTimeSeconds(int moveTimeSeconds) {
        this.moveTimeSeconds = Math.max(1, moveTimeSeconds);
    }
}
