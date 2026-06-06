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

    public MoveResultDto() {
    }

    public MoveResultDto(boolean success, String message, String from, String to,
                         String san, String sideToMove) {
        this(success, message, from, to, san, sideToMove, null, null);
    }

    public MoveResultDto(boolean success, String message, String from, String to,
                         String san, String sideToMove, String position) {
        this(success, message, from, to, san, sideToMove, position, null);
    }

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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public String getSideToMove() {
        return sideToMove;
    }

    public void setSideToMove(String sideToMove) {
        this.sideToMove = sideToMove;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getGameState() {
        return gameState;
    }

    public void setGameState(String gameState) {
        this.gameState = gameState;
    }
}
