package demo.chess.api.dto;

public class MoveResultDto {

    private boolean success;
    private String message;

    private String from;
    private String to;
    private String san;          // short algebraic notation, if available
    private String sideToMove;   // "white" or "black"
    private String position;     // 64-character board position after this move
    private String gameState;    // terminal game state, if the game ended after this move

    /**
     * Creates a new MoveResultDto instance.
     */
    public MoveResultDto() {
    }

    /**
     * Creates a new MoveResultDto instance.
     * @param success the success
     * @param message the message
     * @param from the from
     * @param to the to
     * @param san the san
     * @param sideToMove the side to move
     */
    public MoveResultDto(boolean success, String message, String from, String to,
                         String san, String sideToMove) {
        this(success, message, from, to, san, sideToMove, null, null);
    }

    /**
     * Creates a new MoveResultDto instance.
     * @param success the success
     * @param message the message
     * @param from the from
     * @param to the to
     * @param san the san
     * @param sideToMove the side to move
     * @param position the position
     */
    public MoveResultDto(boolean success, String message, String from, String to,
                         String san, String sideToMove, String position) {
        this(success, message, from, to, san, sideToMove, position, null);
    }

    /**
     * Creates a new MoveResultDto instance.
     * @param success the success
     * @param message the message
     * @param from the from
     * @param to the to
     * @param san the san
     * @param sideToMove the side to move
     * @param position the position
     * @param gameState the game state
     */
    public MoveResultDto(boolean success, String message, String from, String to,
                         String san, String sideToMove, String position, String gameState) {
        this.success = success;
        this.message = message;
        this.from = from;
        this.to = to;
        this.san = san;
        this.sideToMove = sideToMove;
        this.position = position;
        this.gameState = gameState;
    }

    /**
     * Returns whether the success.
     * @return true when the condition is satisfied; otherwise false
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success.
     * @param success the success
     */
    public void setSuccess(boolean success) {
        this.success = success;
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
     * Returns the position.
     * @return the position
     */
    public String getPosition() {
        return position;
    }

    /**
     * Sets the position.
     * @param position the position
     */
    public void setPosition(String position) {
        this.position = position;
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
}
