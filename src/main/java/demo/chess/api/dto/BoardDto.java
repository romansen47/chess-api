package demo.chess.api.dto;

import java.util.List;

public class BoardDto {

    private List<PieceDto> pieces;

    public BoardDto() {
    }

    public BoardDto(List<PieceDto> pieces) {
        this.pieces = pieces;
    }

    public List<PieceDto> getPieces() {
        return pieces;
    }

    public void setPieces(List<PieceDto> pieces) {
        this.pieces = pieces;
    }
}
