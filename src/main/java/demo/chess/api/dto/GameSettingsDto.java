package demo.chess.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import demo.chess.definitions.ChessStartingPosition;

/**
 * Mutable REST DTO describing the settings for a newly created game.
 *
 * <p>{@code startingPositionId} uses Scharnagl Chess960 numbering. Classical
 * chess is position {@value ChessStartingPosition#STANDARD_ID}. Validation and
 * normalization intentionally happen in the application service layer rather
 * than in this Jackson transport object.</p>
 */
public class GameSettingsDto {

    private int timeForEachPlayerSeconds;
    private int incrementForWhiteSeconds;
    private int incrementForBlackSeconds;
    private int additionalTimeAfter40MovesSeconds;
    private String startingColor;
    private int startingPositionId = ChessStartingPosition.STANDARD_ID;
    private long version;

    // Legacy request fields from the former per-game engine-profile selection.
    // Engine profile assignments now live globally under Engine Settings / Defaults.
    @JsonProperty(value = "whiteEngineConfigId", access = JsonProperty.Access.WRITE_ONLY)
    private String legacyWhiteEngineConfigId;

    @JsonProperty(value = "blackEngineConfigId", access = JsonProperty.Access.WRITE_ONLY)
    private String legacyBlackEngineConfigId;

    public GameSettingsDto() {
    }

    /** Compatibility constructor for callers that implicitly request classical chess. */
    public GameSettingsDto(
            int timeForEachPlayerSeconds,
            int incrementForWhiteSeconds,
            int incrementForBlackSeconds,
            int additionalTimeAfter40MovesSeconds,
            String startingColor,
            long version) {
        this(
                timeForEachPlayerSeconds,
                incrementForWhiteSeconds,
                incrementForBlackSeconds,
                additionalTimeAfter40MovesSeconds,
                startingColor,
                ChessStartingPosition.STANDARD_ID,
                version);
    }

    /** Creates a settings DTO including an explicit Chess960 start position. */
    public GameSettingsDto(
            int timeForEachPlayerSeconds,
            int incrementForWhiteSeconds,
            int incrementForBlackSeconds,
            int additionalTimeAfter40MovesSeconds,
            String startingColor,
            int startingPositionId,
            long version) {
        this.timeForEachPlayerSeconds = timeForEachPlayerSeconds;
        this.incrementForWhiteSeconds = incrementForWhiteSeconds;
        this.incrementForBlackSeconds = incrementForBlackSeconds;
        this.additionalTimeAfter40MovesSeconds = additionalTimeAfter40MovesSeconds;
        this.startingColor = startingColor;
        this.startingPositionId = startingPositionId;
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

    public int getStartingPositionId() {
        return startingPositionId;
    }

    public void setStartingPositionId(int startingPositionId) {
        this.startingPositionId = startingPositionId;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
