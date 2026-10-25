package demo.chess.api.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import demo.chess.definitions.ChessStartingPosition;

/**
 * Transport representation of a complete replayable game snapshot.
 *
 * <p>The DTO contains both the current serialized position and the complete
 * move history. {@code startingPositionId} and {@code initialFen} are part of
 * that replay contract: move history alone is insufficient to reconstruct a
 * Chess960 game. Classical chess is Scharnagl position
 * {@value ChessStartingPosition#STANDARD_ID}.</p>
 */
public class UciGameDto {

    private int totalPlies;
    private String sideToMove;
    private String position;
    private List<UciGameMoveDto> moves = new ArrayList<>();
    private String whitePlayerName;
    private String blackPlayerName;
    private Long databaseGameId;
    private List<GameAnnotationDto> annotations = new ArrayList<>();
    private int startingPositionId = ChessStartingPosition.STANDARD_ID;
    private String initialFen = ChessStartingPosition.STANDARD.initialFen();
    private boolean standardStartingPosition = true;
    private Map<String, String> pgnTags = new LinkedHashMap<>();

    /** Creates an empty snapshot for serialization frameworks. */
    public UciGameDto() {
    }

    /** Compatibility constructor for classical games without database metadata. */
    public UciGameDto(
            int totalPlies,
            String sideToMove,
            String position,
            List<UciGameMoveDto> moves,
            String whitePlayerName,
            String blackPlayerName) {
        this(totalPlies, sideToMove, position, moves, whitePlayerName, blackPlayerName,
                null, List.of(), ChessStartingPosition.STANDARD_ID,
                ChessStartingPosition.STANDARD.initialFen());
    }

    /** Compatibility constructor for classical games with metadata. */
    public UciGameDto(
            int totalPlies,
            String sideToMove,
            String position,
            List<UciGameMoveDto> moves,
            String whitePlayerName,
            String blackPlayerName,
            Long databaseGameId,
            List<GameAnnotationDto> annotations) {
        this(totalPlies, sideToMove, position, moves, whitePlayerName, blackPlayerName,
                databaseGameId, annotations, ChessStartingPosition.STANDARD_ID,
                ChessStartingPosition.STANDARD.initialFen());
    }

    /** Creates a replayable snapshot with explicit start-position metadata. */
    public UciGameDto(
            int totalPlies,
            String sideToMove,
            String position,
            List<UciGameMoveDto> moves,
            String whitePlayerName,
            String blackPlayerName,
            Long databaseGameId,
            List<GameAnnotationDto> annotations,
            int startingPositionId,
            String initialFen) {
        this.totalPlies = totalPlies;
        this.sideToMove = sideToMove;
        this.position = position;
        this.moves = moves != null ? new ArrayList<>(moves) : new ArrayList<>();
        this.whitePlayerName = whitePlayerName;
        this.blackPlayerName = blackPlayerName;
        this.databaseGameId = databaseGameId;
        this.annotations = annotations != null ? new ArrayList<>(annotations) : new ArrayList<>();
        this.startingPositionId = startingPositionId;
        this.initialFen = initialFen;
        this.standardStartingPosition = ChessStartingPosition.of(startingPositionId).isStandard();
    }

    /** @return total number of half-moves in the snapshot */
    public int getTotalPlies() { return totalPlies; }
    /** @param totalPlies total number of half-moves */
    public void setTotalPlies(int totalPlies) { this.totalPlies = totalPlies; }
    /** @return side to move */
    public String getSideToMove() { return sideToMove; }
    /** @param sideToMove side to move */
    public void setSideToMove(String sideToMove) { this.sideToMove = sideToMove; }
    /** @return compact current board position */
    public String getPosition() { return position; }
    /** @param position compact current board position */
    public void setPosition(String position) { this.position = position; }
    /** @return replayable move history */
    public List<UciGameMoveDto> getMoves() { return moves; }
    /** @param moves replayable move history */
    public void setMoves(List<UciGameMoveDto> moves) {
        this.moves = moves != null ? new ArrayList<>(moves) : new ArrayList<>();
    }
    /** @return displayed White player name */
    public String getWhitePlayerName() { return whitePlayerName; }
    /** @param whitePlayerName displayed White player name */
    public void setWhitePlayerName(String whitePlayerName) { this.whitePlayerName = whitePlayerName; }
    /** @return displayed Black player name */
    public String getBlackPlayerName() { return blackPlayerName; }
    /** @param blackPlayerName displayed Black player name */
    public void setBlackPlayerName(String blackPlayerName) { this.blackPlayerName = blackPlayerName; }
    /** @return backing database game id, or {@code null} */
    public Long getDatabaseGameId() { return databaseGameId; }
    /** @param databaseGameId backing database game id */
    public void setDatabaseGameId(Long databaseGameId) { this.databaseGameId = databaseGameId; }
    /** @return stored game annotations */
    public List<GameAnnotationDto> getAnnotations() { return annotations; }
    /** @param annotations stored game annotations */
    public void setAnnotations(List<GameAnnotationDto> annotations) {
        this.annotations = annotations != null ? new ArrayList<>(annotations) : new ArrayList<>();
    }
    /** @return Scharnagl starting-position identifier */
    public int getStartingPositionId() { return startingPositionId; }
    /** @param startingPositionId Scharnagl starting-position identifier */
    public void setStartingPositionId(int startingPositionId) { this.startingPositionId = startingPositionId; }
    /** @return explicit initial FEN used for replay */
    public String getInitialFen() { return initialFen; }
    /** @param initialFen explicit initial FEN used for replay */
    public void setInitialFen(String initialFen) { this.initialFen = initialFen; }
    /** @return whether the semantic starting position is classical Scharnagl 518 */
    public boolean isStandardStartingPosition() { return standardStartingPosition; }
    /** @param standardStartingPosition whether the semantic starting position is classical */
    public void setStandardStartingPosition(boolean standardStartingPosition) {
        this.standardStartingPosition = standardStartingPosition;
    }
    /** @return defensive copy of PGN tags */
    public Map<String, String> getPgnTags() { return new LinkedHashMap<>(pgnTags); }
    /** @param pgnTags PGN tags to copy into the snapshot */
    public void setPgnTags(Map<String, String> pgnTags) {
        this.pgnTags = pgnTags == null ? new LinkedHashMap<>() : new LinkedHashMap<>(pgnTags);
    }
}
