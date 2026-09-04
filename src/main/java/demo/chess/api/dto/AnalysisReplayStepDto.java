package demo.chess.api.dto;

import java.util.List;

public class AnalysisReplayStepDto {

    private boolean active;
    private boolean done;
    private int totalPlies;
    private int currentPly;
    private String from;
    private String to;
    private String san;
    private double evaluation;
    private double bar;
    private int depth;
    private String engineName;
    private BoardDto board;
    private List<AnalysisProfilePointDto> profile;
    private String message;

    /**
     * Creates a new AnalysisReplayStepDto instance.
     */
    public AnalysisReplayStepDto() {
    }

    /**
     * Creates a new AnalysisReplayStepDto instance.
     * @param active the active
     * @param done the done
     * @param totalPlies the total plies
     * @param currentPly the current ply
     * @param from the from
     * @param to the to
     * @param san the san
     * @param evaluation the evaluation
     * @param bar the bar
     * @param depth the depth
     * @param engineName the engine name
     * @param board the board
     * @param profile the profile
     * @param message the message
     */
    public AnalysisReplayStepDto(
            boolean active,
            boolean done,
            int totalPlies,
            int currentPly,
            String from,
            String to,
            String san,
            double evaluation,
            double bar,
            int depth,
            String engineName,
            BoardDto board,
            List<AnalysisProfilePointDto> profile,
            String message) {
        this.active = active;
        this.done = done;
        this.totalPlies = totalPlies;
        this.currentPly = currentPly;
        this.from = from;
        this.to = to;
        this.san = san;
        this.evaluation = evaluation;
        this.bar = bar;
        this.depth = depth;
        this.engineName = engineName;
        this.board = board;
        this.profile = profile;
        this.message = message;
    }

    /**
     * Returns whether the active.
     * @return true when the condition is satisfied; otherwise false
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets the active.
     * @param active the active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Returns whether the done.
     * @return true when the condition is satisfied; otherwise false
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Sets the done.
     * @param done the done
     */
    public void setDone(boolean done) {
        this.done = done;
    }

    /**
     * Returns the total plies.
     * @return the total plies
     */
    public int getTotalPlies() {
        return totalPlies;
    }

    /**
     * Sets the total plies.
     * @param totalPlies the total plies
     */
    public void setTotalPlies(int totalPlies) {
        this.totalPlies = totalPlies;
    }

    /**
     * Returns the current ply.
     * @return the current ply
     */
    public int getCurrentPly() {
        return currentPly;
    }

    /**
     * Sets the current ply.
     * @param currentPly the current ply
     */
    public void setCurrentPly(int currentPly) {
        this.currentPly = currentPly;
    }

    /**
     * Returns the from.
     * @return the from
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the from.
     * @param from the from
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Returns the to.
     * @return the to
     */
    public String getTo() {
        return to;
    }

    /**
     * Sets the to.
     * @param to the to
     */
    public void setTo(String to) {
        this.to = to;
    }

    /**
     * Returns the san.
     * @return the san
     */
    public String getSan() {
        return san;
    }

    /**
     * Sets the san.
     * @param san the san
     */
    public void setSan(String san) {
        this.san = san;
    }

    /**
     * Returns the evaluation.
     * @return the evaluation
     */
    public double getEvaluation() {
        return evaluation;
    }

    /**
     * Sets the evaluation.
     * @param evaluation the evaluation
     */
    public void setEvaluation(double evaluation) {
        this.evaluation = evaluation;
    }

    /**
     * Returns the bar.
     * @return the bar
     */
    public double getBar() {
        return bar;
    }

    /**
     * Sets the bar.
     * @param bar the bar
     */
    public void setBar(double bar) {
        this.bar = bar;
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
        this.depth = depth;
    }

    /**
     * Returns the engine name.
     * @return the engine name
     */
    public String getEngineName() {
        return engineName;
    }

    /**
     * Sets the engine name.
     * @param engineName the engine name
     */
    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    /**
     * Returns the board.
     * @return the board
     */
    public BoardDto getBoard() {
        return board;
    }

    /**
     * Sets the board.
     * @param board the board
     */
    public void setBoard(BoardDto board) {
        this.board = board;
    }

    /**
     * Returns the profile.
     * @return the profile
     */
    public List<AnalysisProfilePointDto> getProfile() {
        return profile;
    }

    /**
     * Sets the profile.
     * @param profile the profile
     */
    public void setProfile(List<AnalysisProfilePointDto> profile) {
        this.profile = profile;
    }

    /**
     * Returns the message.
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message.
     * @param message the message
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
