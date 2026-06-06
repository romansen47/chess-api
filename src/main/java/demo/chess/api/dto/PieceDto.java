package demo.chess.api.dto;

public class PieceDto {

    private String color;   // "white" | "black"
    private String type;    // "pawn" | "rook" | "knight" | "bishop" | "queen" | "king"
    private String square;  // z.B. "e4"

    public PieceDto() {
    }

    public PieceDto(String color, String type, String square) {
        this.color = color;
        this.type = type;
        this.square = square;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSquare() {
        return square;
    }

    public void setSquare(String square) {
        this.square = square;
    }
}
