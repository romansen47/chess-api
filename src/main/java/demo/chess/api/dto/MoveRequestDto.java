package demo.chess.api.dto;

public class MoveRequestDto {

    private String from;
    private String to;
    private String promotion; // optional, used for pawn promotion

    public MoveRequestDto() {
    }

    public MoveRequestDto(String from, String to, String promotion) {
        this.from = from;
        this.to = to;
        this.promotion = promotion;
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

    public String getPromotion() {
        return promotion;
    }

    public void setPromotion(String promotion) {
        this.promotion = promotion;
    }
}
