package demo.chess.api.dto;

public class MoveRequestDto {

    private String from;
    private String to;
    private String promotion; // optional, used for pawn promotion

    /**
     * Creates a new MoveRequestDto instance.
     */
    public MoveRequestDto() {
    }

    /**
     * Creates a new MoveRequestDto instance.
     * @param from the from
     * @param to the to
     * @param promotion the promotion
     */
    public MoveRequestDto(String from, String to, String promotion) {
        this.from = from;
        this.to = to;
        this.promotion = promotion;
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
     * Returns the promotion.
     * @return the promotion
     */
    public String getPromotion() {
        return promotion;
    }

    /**
     * Sets the promotion.
     * @param promotion the promotion
     */
    public void setPromotion(String promotion) {
        this.promotion = promotion;
    }
}
