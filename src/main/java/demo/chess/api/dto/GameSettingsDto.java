package demo.chess.api.dto;

public class GameSettingsDto {

    private int timeForEachPlayerSeconds;
    private int incrementForWhiteSeconds;
    private int incrementForBlackSeconds;
    private int additionalTimeAfter40MovesSeconds;
    private String startingColor;
    private long version;

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