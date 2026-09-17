package demo.chess.api.dto;

import java.util.ArrayList;
import java.util.List;

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
    }

    public int getTotalPlies() { return totalPlies; }
    public void setTotalPlies(int totalPlies) { this.totalPlies = totalPlies; }
    public String getSideToMove() { return sideToMove; }
    public void setSideToMove(String sideToMove) { this.sideToMove = sideToMove; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public List<UciGameMoveDto> getMoves() { return moves; }
    public void setMoves(List<UciGameMoveDto> moves) {
        this.moves = moves != null ? new ArrayList<>(moves) : new ArrayList<>();
    }
    public String getWhitePlayerName() { return whitePlayerName; }
    public void setWhitePlayerName(String whitePlayerName) { this.whitePlayerName = whitePlayerName; }
    public String getBlackPlayerName() { return blackPlayerName; }
    public void setBlackPlayerName(String blackPlayerName) { this.blackPlayerName = blackPlayerName; }
    public Long getDatabaseGameId() { return databaseGameId; }
    public void setDatabaseGameId(Long databaseGameId) { this.databaseGameId = databaseGameId; }
    public List<GameAnnotationDto> getAnnotations() { return annotations; }
    public void setAnnotations(List<GameAnnotationDto> annotations) {
        this.annotations = annotations != null ? new ArrayList<>(annotations) : new ArrayList<>();
    }
    public int getStartingPositionId() { return startingPositionId; }
    public void setStartingPositionId(int startingPositionId) { this.startingPositionId = startingPositionId; }
    public String getInitialFen() { return initialFen; }
    public void setInitialFen(String initialFen) { this.initialFen = initialFen; }
}
