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

    /**
     * Creates a new ClockDto instance.
     */
    public ClockDto() {
    }

    /**
     * Creates a new ClockDto instance.
     * @param whiteTime the white time
     * @param blackTime the black time
     * @param sideToMove the side to move
     * @param whiteRunning the white running
     * @param blackRunning the black running
     * @param gameState the game state
     * @param timeControl the time control
     */
    public ClockDto(int whiteTime, int blackTime, String sideToMove,
            boolean whiteRunning, boolean blackRunning,
            String gameState, String timeControl) {
        this(whiteTime, blackTime, sideToMove, whiteRunning, blackRunning,
                gameState, timeControl, null, null, null, null);
    }

    /**
     * Creates a new ClockDto instance.
     * @param whiteTime the white time
     * @param blackTime the black time
     * @param sideToMove the side to move
     * @param whiteRunning the white running
     * @param blackRunning the black running
     * @param gameState the game state
     * @param timeControl the time control
     * @param whitePlayerName the white player name
     * @param blackPlayerName the black player name
     */
    public ClockDto(int whiteTime, int blackTime, String sideToMove,
            boolean whiteRunning, boolean blackRunning,
            String gameState, String timeControl,
            String whitePlayerName, String blackPlayerName) {
        this(whiteTime, blackTime, sideToMove, whiteRunning, blackRunning,
                gameState, timeControl, whitePlayerName, blackPlayerName, null, null);
    }

    /**
     * Creates a new ClockDto instance.
     * @param whiteTime the white time
     * @param blackTime the black time
     * @param sideToMove the side to move
     * @param whiteRunning the white running
     * @param blackRunning the black running
     * @param gameState the game state
     * @param timeControl the time control
     * @param whitePlayerName the white player name
     * @param blackPlayerName the black player name
     * @param whitePlayerEngineName the white player engine name
     * @param blackPlayerEngineName the black player engine name
     */
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

    /**
     * Returns the white time.
     * @return the white time
     */
    public int getWhiteTime() {
        return whiteTime;
    }

    /**
     * Sets the white time.
     * @param whiteTime the white time
     */
    public void setWhiteTime(int whiteTime) {
        this.whiteTime = whiteTime;
    }

    /**
     * Returns the black time.
     * @return the black time
     */
    public int getBlackTime() {
        return blackTime;
    }

    /**
     * Sets the black time.
     * @param blackTime the black time
     */
    public void setBlackTime(int blackTime) {
        this.blackTime = blackTime;
    }

    /**
     * Returns the side to move.
     * @return the side to move
     */
    public String getSideToMove() {
        return sideToMove;
    }

    /**
     * Sets the side to move.
     * @param sideToMove the side to move
     */
    public void setSideToMove(String sideToMove) {
        this.sideToMove = sideToMove;
    }

    /**
     * Returns whether the white running.
     * @return true when the condition is satisfied; otherwise false
     */
    public boolean isWhiteRunning() {
        return whiteRunning;
    }

    /**
     * Sets the white running.
     * @param whiteRunning the white running
     */
    public void setWhiteRunning(boolean whiteRunning) {
        this.whiteRunning = whiteRunning;
    }

    /**
     * Returns whether the black running.
     * @return true when the condition is satisfied; otherwise false
     */
    public boolean isBlackRunning() {
        return blackRunning;
    }

    /**
     * Sets the black running.
     * @param blackRunning the black running
     */
    public void setBlackRunning(boolean blackRunning) {
        this.blackRunning = blackRunning;
    }

    /**
     * Returns the game state.
     * @return the game state
     */
    public String getGameState() {
        return gameState;
    }

    /**
     * Sets the game state.
     * @param gameState the game state
     */
    public void setGameState(String gameState) {
        this.gameState = gameState;
    }

    /**
     * Returns the time control.
     * @return the time control
     */
    public String getTimeControl() {
        return timeControl;
    }

    /**
     * Sets the time control.
     * @param timeControl the time control
     */
    public void setTimeControl(String timeControl) {
        this.timeControl = timeControl;
    }

    /**
     * Returns the white player name.
     * @return the white player name
     */
    public String getWhitePlayerName() {
        return whitePlayerName;
    }

    /**
     * Sets the white player name.
     * @param whitePlayerName the white player name
     */
    public void setWhitePlayerName(String whitePlayerName) {
        this.whitePlayerName = whitePlayerName;
    }

    /**
     * Returns the black player name.
     * @return the black player name
     */
    public String getBlackPlayerName() {
        return blackPlayerName;
    }

    /**
     * Sets the black player name.
     * @param blackPlayerName the black player name
     */
    public void setBlackPlayerName(String blackPlayerName) {
        this.blackPlayerName = blackPlayerName;
    }
    /**
     * Returns the white player engine name.
     * @return the white player engine name
     */
    public String getWhitePlayerEngineName() {
        return whitePlayerEngineName;
    }

    /**
     * Sets the white player engine name.
     * @param whitePlayerEngineName the white player engine name
     */
    public void setWhitePlayerEngineName(String whitePlayerEngineName) {
        this.whitePlayerEngineName = whitePlayerEngineName;
    }

    /**
     * Returns the black player engine name.
     * @return the black player engine name
     */
    public String getBlackPlayerEngineName() {
        return blackPlayerEngineName;
    }

    /**
     * Sets the black player engine name.
     * @param blackPlayerEngineName the black player engine name
     */
    public void setBlackPlayerEngineName(String blackPlayerEngineName) {
        this.blackPlayerEngineName = blackPlayerEngineName;
    }
}

