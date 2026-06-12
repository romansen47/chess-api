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
    private BoardDto board;
    private List<AnalysisProfilePointDto> profile;
    private String message;

    public AnalysisReplayStepDto() {
    }

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
        this.board = board;
        this.profile = profile;
        this.message = message;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public int getTotalPlies() {
        return totalPlies;
    }

    public void setTotalPlies(int totalPlies) {
        this.totalPlies = totalPlies;
    }

    public int getCurrentPly() {
        return currentPly;
    }

    public void setCurrentPly(int currentPly) {
        this.currentPly = currentPly;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSan() {
        return san;
    }

    public void setSan(String san) {
        this.san = san;
    }

    public double getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(double evaluation) {
        this.evaluation = evaluation;
    }

    public double getBar() {
        return bar;
    }

    public void setBar(double bar) {
        this.bar = bar;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public BoardDto getBoard() {
        return board;
    }

    public void setBoard(BoardDto board) {
        this.board = board;
    }

    public List<AnalysisProfilePointDto> getProfile() {
        return profile;
    }

    public void setProfile(List<AnalysisProfilePointDto> profile) {
        this.profile = profile;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
