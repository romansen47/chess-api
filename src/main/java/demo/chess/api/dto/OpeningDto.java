package demo.chess.api.dto;

import java.util.ArrayList;
import java.util.List;

/** REST representation of one reusable opening line. */
public class OpeningDto {

    private String id;
    private String name;
    private String family;
    private String variation;
    private String eco;
    private int startingPositionId;
    private String initialFen;
    private List<String> moves = new ArrayList<>();
    private List<String> sanMoves = new ArrayList<>();
    private String position;
    private String sideToMove;
    private boolean playable;

    public OpeningDto() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFamily() { return family; }
    public void setFamily(String family) { this.family = family; }
    public String getVariation() { return variation; }
    public void setVariation(String variation) { this.variation = variation; }
    public String getEco() { return eco; }
    public void setEco(String eco) { this.eco = eco; }
    public int getStartingPositionId() { return startingPositionId; }
    public void setStartingPositionId(int startingPositionId) { this.startingPositionId = startingPositionId; }
    public String getInitialFen() { return initialFen; }
    public void setInitialFen(String initialFen) { this.initialFen = initialFen; }
    public List<String> getMoves() { return moves; }
    public void setMoves(List<String> moves) {
        this.moves = moves == null ? new ArrayList<>() : new ArrayList<>(moves);
    }
    public List<String> getSanMoves() { return sanMoves; }
    public void setSanMoves(List<String> sanMoves) {
        this.sanMoves = sanMoves == null ? new ArrayList<>() : new ArrayList<>(sanMoves);
    }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getSideToMove() { return sideToMove; }
    public void setSideToMove(String sideToMove) { this.sideToMove = sideToMove; }
    public boolean isPlayable() { return playable; }
    public void setPlayable(boolean playable) { this.playable = playable; }
}
