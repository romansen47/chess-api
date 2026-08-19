package demo.chess.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GameSettingsDto {

    private int timeForEachPlayerSeconds;
    private int incrementForWhiteSeconds;
    private int incrementForBlackSeconds;
    private int additionalTimeAfter40MovesSeconds;
    private String startingColor;
    private long version;

    // Legacy request fields from the former per-game engine-profile selection.
    // Engine profile assignments now live globally under Engine Settings / Defaults.
    @JsonProperty(value = "whiteEngineConfigId", access = JsonProperty.Access.WRITE_ONLY)
    private String legacyWhiteEngineConfigId;

    @JsonProperty(value = "blackEngineConfigId", access = JsonProperty.Access.WRITE_ONLY)
    private String legacyBlackEngineConfigId;

    public GameSettingsDto() {
    }

    public GameSettingsDto(int timeForEachPlayerSeconds,
            int incrementForWhiteSeconds,
            int incrementForBlackSeconds,
            int additionalTimeAfter40MovesSeconds,
            String startingColor,
            long version) {
        this.timeForEachPlayerSeconds = timeForEachPlayerSeconds;
        this.incrementForWhiteSeconds = incrementForWhiteSeconds;
        this.incrementForBlackSeconds = incrementForBlackSeconds;
        this.additionalTimeAfter40MovesSeconds = additionalTimeAfter40MovesSeconds;
        this.startingColor = startingColor;
        this.version = version;
    }

    public int getTimeForEachPlayerSeconds() {
        return timeForEachPlayerSeconds;
    }

    public void setTimeForEachPlayerSeconds(int timeForEachPlayerSeconds) {
        this.timeForEachPlayerSeconds = timeForEachPlayerSeconds;
    }

    public int getIncrementForWhiteSeconds() {
        return incrementForWhiteSeconds;
    }

    public void setIncrementForWhiteSeconds(int incrementForWhiteSeconds) {
        this.incrementForWhiteSeconds = incrementForWhiteSeconds;
    }

    public int getIncrementForBlackSeconds() {
        return incrementForBlackSeconds;
    }

    public void setIncrementForBlackSeconds(int incrementForBlackSeconds) {
        this.incrementForBlackSeconds = incrementForBlackSeconds;
    }

    public int getAdditionalTimeAfter40MovesSeconds() {
        return additionalTimeAfter40MovesSeconds;
    }

    public void setAdditionalTimeAfter40MovesSeconds(int additionalTimeAfter40MovesSeconds) {
        this.additionalTimeAfter40MovesSeconds = additionalTimeAfter40MovesSeconds;
    }

    public String getStartingColor() {
        return startingColor;
    }

    public void setStartingColor(String startingColor) {
        this.startingColor = startingColor;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
