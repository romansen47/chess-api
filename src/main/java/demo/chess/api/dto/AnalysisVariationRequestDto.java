package demo.chess.api.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a temporary analysis variation relative to the original game.
 *
 * <p>The original position is identified by {@code anchorPly}. The variation
 * itself is represented exclusively by the ordered UCI moves that follow that
 * anchor. Optional move fields are used by the move/possible-move endpoints.</p>
 */
public class AnalysisVariationRequestDto {

    private int anchorPly;
    private List<String> moves = new ArrayList<>();
    private String from;
    private String to;
    private String promotion;

    /**
     * Creates a new AnalysisVariationRequestDto instance.
     */
    public AnalysisVariationRequestDto() {
    }

    /**
     * Returns the anchor ply.
     * @return the anchor ply
     */
    public int getAnchorPly() {
        return anchorPly;
    }

    /**
     * Sets the anchor ply.
     * @param anchorPly the anchor ply
     */
    public void setAnchorPly(int anchorPly) {
        this.anchorPly = anchorPly;
    }

    /**
     * Returns the variation moves.
     * @return the variation moves
     */
    public List<String> getMoves() {
        return moves;
    }

    /**
     * Sets the variation moves.
     * @param moves the variation moves
     */
    public void setMoves(List<String> moves) {
        this.moves = moves != null ? new ArrayList<>(moves) : new ArrayList<>();
    }

    /**
     * Returns the source square.
     * @return the source square
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the source square.
     * @param from the source square
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Returns the target square.
     * @return the target square
     */
    public String getTo() {
        return to;
    }

    /**
     * Sets the target square.
     * @param to the target square
     */
    public void setTo(String to) {
        this.to = to;
    }

    /**
     * Returns the requested promotion.
     * @return the requested promotion
     */
    public String getPromotion() {
        return promotion;
    }

    /**
     * Sets the requested promotion.
     * @param promotion the requested promotion
     */
    public void setPromotion(String promotion) {
        this.promotion = promotion;
    }
}
