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
    private Long databaseGameId;
    private List<GameAnnotationDto> annotations = new ArrayList<>();

    /**
     * Creates a new UciGameDto instance.
     */
    public UciGameDto() {
    }

    /**
     * Creates a new UciGameDto instance.
     * @param totalPlies the total plies
     * @param sideToMove the side to move
     * @param position the position
     * @param moves the moves
     * @param whitePlayerName the white player name
     * @param blackPlayerName the black player name
     */
    public UciGameDto(
            int totalPlies,
            String sideToMove,
            String position,
            List<UciGameMoveDto> moves,
            String whitePlayerName,
            String blackPlayerName) {
        this(totalPlies, sideToMove, position, moves, whitePlayerName, blackPlayerName, null, List.of());
    }

    /**
     * Creates a game DTO including optional persisted PGN annotations.
     */
    public UciGameDto(
            int totalPlies,
            String sideToMove,
            String position,
            List<UciGameMoveDto> moves,
            String whitePlayerName,
            String blackPlayerName,
            Long databaseGameId,
            List<GameAnnotationDto> annotations) {
        this.totalPlies = totalPlies;
        this.sideToMove = sideToMove;
        this.position = position;
        this.moves = moves != null ? moves : new ArrayList<>();
        this.whitePlayerName = whitePlayerName;
        this.blackPlayerName = blackPlayerName;
        this.databaseGameId = databaseGameId;
        this.annotations = annotations != null ? new ArrayList<>(annotations) : new ArrayList<>();
    }

    /**
     * Returns the total plies.
     * @return the total plies
     */
    public int getTotalPlies() {
        return totalPlies;
    }

    /**
     * Sets the total plies.
     * @param totalPlies the total plies
     */
    public void setTotalPlies(int totalPlies) {
        this.totalPlies = totalPlies;
    }

    /**
     * Returns the side to move.
     * @return the side to move
     */
    public String getSideToMove() {
        return sideToMove;
    }

    /**
     * Sets the side to move.
     * @param sideToMove the side to move
     */
    public void setSideToMove(String sideToMove) {
        this.sideToMove = sideToMove;
    }

    /**
     * Returns the position.
     * @return the position
     */
    public String getPosition() {
        return position;
    }

    /**
     * Sets the position.
     * @param position the position
     */
    public void setPosition(String position) {
        this.position = position;
    }

    /**
     * Returns the moves.
     * @return the moves
     */
    public List<UciGameMoveDto> getMoves() {
        return moves;
    }

    /**
     * Sets the moves.
     * @param moves the moves
     */
    public void setMoves(List<UciGameMoveDto> moves) {
        this.moves = moves;
    }

    /**
     * Returns the white player name.
     * @return the white player name
     */
    public String getWhitePlayerName() {
        return whitePlayerName;
    }

    /**
     * Sets the white player name.
     * @param whitePlayerName the white player name
     */
    public void setWhitePlayerName(String whitePlayerName) {
        this.whitePlayerName = whitePlayerName;
    }

    /**
     * Returns the black player name.
     * @return the black player name
     */
    public String getBlackPlayerName() {
        return blackPlayerName;
    }

    /**
     * Sets the black player name.
     * @param blackPlayerName the black player name
     */
    public void setBlackPlayerName(String blackPlayerName) {
        this.blackPlayerName = blackPlayerName;
    }

    public Long getDatabaseGameId() {
        return databaseGameId;
    }

    public void setDatabaseGameId(Long databaseGameId) {
        this.databaseGameId = databaseGameId;
    }

    public List<GameAnnotationDto> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<GameAnnotationDto> annotations) {
        this.annotations = annotations != null ? new ArrayList<>(annotations) : new ArrayList<>();
    }
}
