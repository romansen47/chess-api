package demo.chess.api.dto;

import java.util.List;

public class BoardDto {

    private List<PieceDto> pieces;

    /**
     * Creates a new BoardDto instance.
     */
    public BoardDto() {
    }

    /**
     * Creates a new BoardDto instance.
     * @param pieces the pieces
     */
    public BoardDto(List<PieceDto> pieces) {
        this.pieces = pieces;
    }

    /**
     * Returns the pieces.
     * @return the pieces
     */
    public List<PieceDto> getPieces() {
        return pieces;
    }

    /**
     * Sets the pieces.
     * @param pieces the pieces
     */
    public void setPieces(List<PieceDto> pieces) {
        this.pieces = pieces;
    }
}
