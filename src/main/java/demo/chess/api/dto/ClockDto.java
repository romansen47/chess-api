package demo.chess.api.dto;

public class ClockDto {

    private int whiteTime;
    private int blackTime;
    private String sideToMove;
    private boolean whiteRunning;
    private boolean blackRunning;
    private String gameState;
    private String timeControl;

    public ClockDto() {
    }

    public ClockDto(int whiteTime, int blackTime, String sideToMove,
            boolean whiteRunning, boolean blackRunning,
            String gameState, String timeControl) {
        this.whiteTime = whiteTime;
        this.blackTime = blackTime;
        this.sideToMove = sideToMove;
        this.whiteRunning = whiteRunning;
        this.blackRunning = blackRunning;
        this.gameState = gameState;
        this.timeControl = timeControl;
    }

    public int getWhiteTime() {
        return whiteTime;
    }

    public void setWhiteTime(int whiteTime) {
        this.whiteTime = whiteTime;
    }

    public int getBlackTime() {
        return blackTime;
    }

    public void setBlackTime(int blackTime) {
        this.blackTime = blackTime;
    }

    public String getSideToMove() {
        return sideToMove;
    }

    public void setSideToMove(String sideToMove) {
        this.sideToMove = sideToMove;
    }

    public boolean isWhiteRunning() {
        return whiteRunning;
    }

    public void setWhiteRunning(boolean whiteRunning) {
        this.whiteRunning = whiteRunning;
    }

    public boolean isBlackRunning() {
        return blackRunning;
    }

    public void setBlackRunning(boolean blackRunning) {
        this.blackRunning = blackRunning;
    }

    public String getGameState() {
        return gameState;
    }

    public void setGameState(String gameState) {
        this.gameState = gameState;
    }

    public String getTimeControl() {
        return timeControl;
    }

    public void setTimeControl(String timeControl) {
        this.timeControl = timeControl;
    }
}