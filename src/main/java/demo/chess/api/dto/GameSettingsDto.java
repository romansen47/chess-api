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

    /**
     * Creates a new GameSettingsDto instance.
     */
    public GameSettingsDto() {
    }

    /**
     * Creates a new GameSettingsDto instance.
     * @param timeForEachPlayerSeconds the time for each player seconds
     * @param incrementForWhiteSeconds the increment for white seconds
     * @param incrementForBlackSeconds the increment for black seconds
     * @param additionalTimeAfter40MovesSeconds the additional time after40 moves seconds
     * @param startingColor the starting color
     * @param version the version
     */
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

    /**
     * Returns the time for each player seconds.
     * @return the time for each player seconds
     */
    public int getTimeForEachPlayerSeconds() {
        return timeForEachPlayerSeconds;
    }

    /**
     * Sets the time for each player seconds.
     * @param timeForEachPlayerSeconds the time for each player seconds
     */
    public void setTimeForEachPlayerSeconds(int timeForEachPlayerSeconds) {
        this.timeForEachPlayerSeconds = timeForEachPlayerSeconds;
    }

    /**
     * Returns the increment for white seconds.
     * @return the increment for white seconds
     */
    public int getIncrementForWhiteSeconds() {
        return incrementForWhiteSeconds;
    }

    /**
     * Sets the increment for white seconds.
     * @param incrementForWhiteSeconds the increment for white seconds
     */
    public void setIncrementForWhiteSeconds(int incrementForWhiteSeconds) {
        this.incrementForWhiteSeconds = incrementForWhiteSeconds;
    }

    /**
     * Returns the increment for black seconds.
     * @return the increment for black seconds
     */
    public int getIncrementForBlackSeconds() {
        return incrementForBlackSeconds;
    }

    /**
     * Sets the increment for black seconds.
     * @param incrementForBlackSeconds the increment for black seconds
     */
    public void setIncrementForBlackSeconds(int incrementForBlackSeconds) {
        this.incrementForBlackSeconds = incrementForBlackSeconds;
    }

    /**
     * Returns the additional time after40 moves seconds.
     * @return the additional time after40 moves seconds
     */
    public int getAdditionalTimeAfter40MovesSeconds() {
        return additionalTimeAfter40MovesSeconds;
    }

    /**
     * Sets the additional time after40 moves seconds.
     * @param additionalTimeAfter40MovesSeconds the additional time after40 moves seconds
     */
    public void setAdditionalTimeAfter40MovesSeconds(int additionalTimeAfter40MovesSeconds) {
        this.additionalTimeAfter40MovesSeconds = additionalTimeAfter40MovesSeconds;
    }

    /**
     * Returns the starting color.
     * @return the starting color
     */
    public String getStartingColor() {
        return startingColor;
    }

    /**
     * Sets the starting color.
     * @param startingColor the starting color
     */
    public void setStartingColor(String startingColor) {
        this.startingColor = startingColor;
    }

    /**
     * Returns the version.
     * @return the version
     */
    public long getVersion() {
        return version;
    }

    /**
     * Sets the version.
     * @param version the version
     */
    public void setVersion(long version) {
        this.version = version;
    }
}
