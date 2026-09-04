package demo.chess.api.dto;

public class PieceDto {

    private String color;   // "white" | "black"
    private String type;    // "pawn" | "rook" | "knight" | "bishop" | "queen" | "king"
    private String square;  // z.B. "e4"

    /**
     * Creates a new PieceDto instance.
     */
    public PieceDto() {
    }

    /**
     * Creates a new PieceDto instance.
     * @param color the color
     * @param type the type
     * @param square the square
     */
    public PieceDto(String color, String type, String square) {
        this.color = color;
        this.type = type;
        this.square = square;
    }

    /**
     * Returns the color.
     * @return the color
     */
    public String getColor() {
        return color;
    }

    /**
     * Sets the color.
     * @param color the color
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * Returns the type.
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the square.
     * @return the square
     */
    public String getSquare() {
        return square;
    }

    /**
     * Sets the square.
     * @param square the square
     */
    public void setSquare(String square) {
        this.square = square;
    }
}
