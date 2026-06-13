package demo.chess.api.dto;

public class ClockDto {

    private int whiteTime;
    private int blackTime;
    private String sideToMove;
    private boolean whiteRunning;
    private boolean blackRunning;
    private String gameState;
    private String timeControl;
    private String whitePlayerName;
    private String blackPlayerName;
    private String whitePlayerEngineName;
    private String blackPlayerEngineName;

    public ClockDto() {
    }

    public ClockDto(int whiteTime, int blackTime, String sideToMove,
            boolean whiteRunning, boolean blackRunning,
            String gameState, String timeControl) {
        this(whiteTime, blackTime, sideToMove, whiteRunning, blackRunning,
                gameState, timeControl, null, null, null, null);
    }

    public ClockDto(int whiteTime, int blackTime, String sideToMove,
            boolean whiteRunning, boolean blackRunning,
            String gameState, String timeControl,
            String whitePlayerName, String blackPlayerName) {
        this(whiteTime, blackTime, sideToMove, whiteRunning, blackRunning,
                gameState, timeControl, whitePlayerName, blackPlayerName, null, null);
    }

    public ClockDto(int whiteTime, int blackTime, String sideToMove,
            boolean whiteRunning, boolean blackRunning,
            String gameState, String timeControl,
            String whitePlayerName, String blackPlayerName,
            String whitePlayerEngineName, String blackPlayerEngineName) {
        this.whiteTime = whiteTime;
        this.blackTime = blackTime;
        this.sideToMove = sideToMove;
        this.whiteRunning = whiteRunning;
        this.blackRunning = blackRunning;
        this.gameState = gameState;
        this.timeControl = timeControl;
        this.whitePlayerName = whitePlayerName;
        this.blackPlayerName = blackPlayerName;
        this.whitePlayerEngineName = whitePlayerEngineName;
        this.blackPlayerEngineName = blackPlayerEngineName;
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

    public String getWhitePlayerName() {
        return whitePlayerName;
    }

    public void setWhitePlayerName(String whitePlayerName) {
        this.whitePlayerName = whitePlayerName;
    }

    public String getBlackPlayerName() {
        return blackPlayerName;
    }

    public void setBlackPlayerName(String blackPlayerName) {
        this.blackPlayerName = blackPlayerName;
    }
    public String getWhitePlayerEngineName() {
        return whitePlayerEngineName;
    }

    public void setWhitePlayerEngineName(String whitePlayerEngineName) {
        this.whitePlayerEngineName = whitePlayerEngineName;
    }

    public String getBlackPlayerEngineName() {
        return blackPlayerEngineName;
    }

    public void setBlackPlayerEngineName(String blackPlayerEngineName) {
        this.blackPlayerEngineName = blackPlayerEngineName;
    }
}

