package demo.chess.api.dto;

import java.util.ArrayList;
import java.util.List;

public class UciGameDto {

    private int totalPlies;
    private String sideToMove;
    private String position;
    private List<UciGameMoveDto> moves = new ArrayList<>();

    public UciGameDto() {
    }

    public UciGameDto(int totalPlies, String sideToMove, String position, List<UciGameMoveDto> moves) {
        this.totalPlies = totalPlies;
        this.sideToMove = sideToMove;
        this.position = position;
        this.moves = moves != null ? moves : new ArrayList<>();
    }

    public int getTotalPlies() {
        return totalPlies;
    }

    public void setTotalPlies(int totalPlies) {
        this.totalPlies = totalPlies;
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

    public List<UciGameMoveDto> getMoves() {
        return moves;
    }

    public void setMoves(List<UciGameMoveDto> moves) {
        this.moves = moves;
    }
}
