package demo.chess.api.dto;

import java.util.ArrayList;
import java.util.List;

public class UciGameDto {

    private int totalPlies;
    private String sideToMove;
    private String position;
    private List<UciGameMoveDto> moves = new ArrayList<>();
    private String whitePlayerName;
    private String blackPlayerName;

    public UciGameDto() {
    }

    public UciGameDto(
            int totalPlies,
            String sideToMove,
            String position,
            List<UciGameMoveDto> moves,
            String whitePlayerName,
            String blackPlayerName) {
        this.totalPlies = totalPlies;
        this.sideToMove = sideToMove;
        this.position = position;
        this.moves = moves != null ? moves : new ArrayList<>();
        this.whitePlayerName = whitePlayerName;
        this.blackPlayerName = blackPlayerName;
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

    public String getWhitePlayerName() {
        return whitePlayerName;
    }

    public void setWhitePlayerName(String whitePlayerName) {
        this.whitePlayerName = whitePlayerName;
    }

    public String getBlackPlayerName() {
        return blackPlayerName;
    }

    public void setBlackPlayerName(String blackPlayerName) {
        this.blackPlayerName = blackPlayerName;
    }
}
