package demo.chess.api.dto;

/**
 * Result of applying one move to a temporary analysis variation.
 */
public class AnalysisVariationMoveResultDto {

    private boolean success;
    private String message;
    private String from;
    private String to;
    private String uci;
    private String sideToMove;
    private String position;
    private String gameState;

    /**
     * Creates a new AnalysisVariationMoveResultDto instance.
     */
    public AnalysisVariationMoveResultDto() {
    }

    /**
     * Creates a new AnalysisVariationMoveResultDto instance.
     * @param success whether the move succeeded
     * @param message optional error message
     * @param from source square
     * @param to target square
     * @param uci applied UCI move
     * @param sideToMove side to move after the move
     * @param position 64-character board position
     * @param gameState terminal state, if any
     */
    public AnalysisVariationMoveResultDto(
            boolean success,
            String message,
            String from,
            String to,
            String uci,
            String sideToMove,
            String position,
            String gameState) {
        this.success = success;
        this.message = message;
        this.from = from;
        this.to = to;
        this.uci = uci;
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

    public String getUci() {
        return uci;
    }

    public void setUci(String uci) {
        this.uci = uci;
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
