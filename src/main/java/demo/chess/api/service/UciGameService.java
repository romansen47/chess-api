package demo.chess.api.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.UciGameDto;
import demo.chess.api.dto.UciGameMoveDto;
import demo.chess.definitions.Color;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.states.State;
import demo.chess.game.DummyGame;
import demo.chess.game.Game;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;
import demo.chess.notation.PgnNotation;
import demo.chess.save.GameSaver;

@Service
public class UciGameService {

    private static final DateTimeFormatter PGN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final GameService gameService;
    private final EngineSettingsService engineSettingsService;
    private final GameLoader gameLoader = new GameLoader();
    private final GameSaver gameSaver = new GameSaver();

    /**
     * Optional analysis-only game loaded from a PGN file. It deliberately does not
     * replace GameService's live game and therefore never starts the live clocks.
     */
    private Game importedAnalysisGame;
    private Map<String, String> importedPgnTags = new LinkedHashMap<>();

    /**
     * Creates a new UciGameService instance.
     * @param gameService the game service
     * @param engineSettingsService the engine settings service
     */
    public UciGameService(GameService gameService, EngineSettingsService engineSettingsService) {
        this.gameService = gameService;
        this.engineSettingsService = engineSettingsService;
    }

    /**
     * Performs the import game operation.
     * @param content the content
     * @return the result of the operation
     */
    public synchronized UciGameDto importGame(String content) throws NoMoveFoundException, IOException {
        List<String> uciMoves = gameLoader.parsePgnMoveList(content);

        Simulation importedGame = Simulation.createSimulation();
        gameLoader.loadGame(uciMoves, importedGame);

        List<UciGameMoveDto> moveDtos = createMoveDtos(importedGame.getMoveList());
        Map<String, String> pgnTags = new LinkedHashMap<>(gameLoader.parsePgnTags(content));
        this.importedAnalysisGame = importedGame;
        this.importedPgnTags = pgnTags;

        String sideToMove = importedGame.getPlayer() != null && importedGame.getPlayer().getColor() != null
                ? importedGame.getPlayer().getColor().name().toLowerCase(Locale.ROOT)
                : null;

        return new UciGameDto(
                importedGame.getMoveList().size(),
                sideToMove,
                toPositionString(importedGame),
                moveDtos,
                playerName(pgnTags.get("White"), "White"),
                playerName(pgnTags.get("Black"), "Black"));
    }

    /**
     * Performs the export game operation.
     * @param whiteComputerControlled the white computer controlled
     * @param blackComputerControlled the black computer controlled
     * @return the result of the operation
     */
    public synchronized String exportGame(boolean whiteComputerControlled, boolean blackComputerControlled)
            throws NoMoveFoundException, IOException {
        return gameSaver.toPgn(
                getAnalysisMoveListSnapshot(),
                getPgnTagsForExport(whiteComputerControlled, blackComputerControlled));
    }

    /**
     * Returns the analysis move list snapshot.
     * @return the analysis move list snapshot
     */
    public synchronized List<Move> getAnalysisMoveListSnapshot() {
        if (importedAnalysisGame != null) {
            return new ArrayList<>(importedAnalysisGame.getMoveList());
        }
        return gameService.getMoveListSnapshot();
    }

    /**
     * Returns whether this object has the imported game.
     * @return true when the condition is satisfied; otherwise false
     */
    public synchronized boolean hasImportedGame() {
        return importedAnalysisGame != null;
    }

    /**
     * Clears the imported game.
     */
    public synchronized void clearImportedGame() {
        importedAnalysisGame = null;
        importedPgnTags = new LinkedHashMap<>();
    }

    /**
     * Creates the move dtos.
     * @param originalMoves the original moves
     * @return the result of the operation
     */
    private List<UciGameMoveDto> createMoveDtos(List<Move> originalMoves)
            throws NoMoveFoundException, IOException {
        List<UciGameMoveDto> result = new ArrayList<>();
        Simulation replayGame = Simulation.createSimulation();
        DummyGame notationGame = Simulation.createDummySimulation();

        int ply = 0;
        for (Move originalMove : originalMoves) {
            ply++;

            Move replayMove = replayGame.getPlayer().getMoveInSimulation(replayGame, originalMove);
            Move notationMove = notationGame.getPlayer().getMoveInSimulation(notationGame, originalMove);
            String san = PgnNotation.toDisplayNotation(notationGame, notationMove);

            replayGame.apply(replayMove);
            notationGame.apply(notationMove);

            result.add(new UciGameMoveDto(
                    ply,
                    originalMove.toString(),
                    san,
                    toPositionString(replayGame)));
        }

        return result;
    }

    /**
     * Returns the pgn tags for export.
     * @param whiteComputerControlled the white computer controlled
     * @param blackComputerControlled the black computer controlled
     * @return the pgn tags for export
     */
    private Map<String, String> getPgnTagsForExport(
            boolean whiteComputerControlled,
            boolean blackComputerControlled) {
        if (importedAnalysisGame != null) {
            return new LinkedHashMap<>(importedPgnTags);
        }

        Game game = gameService.getCurrentGame();
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("Event", "Chess Frontend");
        tags.put("Site", "?");
        tags.put("Date", LocalDate.now().format(PGN_DATE_FORMAT));
        tags.put("Round", "-");
        tags.put("White", playerNameForExport(game, Color.WHITE, whiteComputerControlled));
        tags.put("Black", playerNameForExport(game, Color.BLACK, blackComputerControlled));
        tags.put("Result", gameResult(game));

        if (game != null && game.getIncrementForWhite() == game.getIncrementForBlack()) {
            tags.put("TimeControl", game.getTimeForEachPlayer() + "+" + game.getIncrementForWhite());
        }
        return tags;
    }

    /**
     * Performs the player name for export operation.
     * @param game the game
     * @param color the color
     * @param computerControlled the computer controlled
     * @return the result of the operation
     */
    private String playerNameForExport(Game game, Color color, boolean computerControlled) {
        String fallback = color == Color.WHITE ? "White" : "Black";

        if (computerControlled) {
            String engineName = color == Color.WHITE
                    ? engineSettingsService.getWhitePlayerEngineName()
                    : engineSettingsService.getBlackPlayerEngineName();
            return playerName(engineName, fallback + " Engine");
        }

        String gamePlayerName = null;
        if (game != null) {
            gamePlayerName = color == Color.WHITE
                    ? game.getWhitePlayer().getName()
                    : game.getBlackPlayer().getName();
        }
        return playerName(gamePlayerName, fallback);
    }

    /**
     * Performs the player name operation.
     * @param name the name
     * @param fallback the fallback
     * @return the result of the operation
     */
    private String playerName(String name, String fallback) {
        if (name == null || name.isBlank()
                || "ChessGame".equals(name)
                || "Simulation".equals(name)) {
            return fallback;
        }
        return name;
    }

    /**
     * Performs the game result operation.
     * @param game the game
     * @return the result of the operation
     */
    private String gameResult(Game game) {
        if (game == null || game.getState() == null) {
            return "*";
        }

        State state = game.getState();
        if (state == State.BLACK_MATED || state == State.BLACK_RESIGNED) {
            return "1-0";
        }
        if (state == State.WHITE_MATED || state == State.WHITE_RESIGNED) {
            return "0-1";
        }
        if (state == State.STALEMATE
                || state == State.DRAW_BY_50_MOVES_RULE
                || state == State.DRAW_BY_THREEFOLD_REPETITION) {
            return "1/2-1/2";
        }
        if (state == State.LOST_ON_TIME && game.getPlayer() != null) {
            return game.getPlayer().getColor() == Color.WHITE ? "0-1" : "1-0";
        }
        return "*";
    }

    /**
     * Performs the to position string operation.
     * @param game the game
     * @return the result of the operation
     */
    private String toPositionString(Game game) {
        Board board = game.getChessBoard();
        StringBuilder position = new StringBuilder(64);

        for (int rank = 8; rank >= 1; rank--) {
            for (int file = 1; file <= 8; file++) {
                Field field = board.getField(file, rank);
                Piece piece = field != null ? field.getPiece() : null;
                position.append(toPositionChar(piece));
            }
        }

        return position.toString();
    }

    /**
     * Performs the to position char operation.
     * @param piece the piece
     * @return the result of the operation
     */
    private char toPositionChar(Piece piece) {
        if (piece == null || piece.getType() == null) {
            return '.';
        }

        char pieceChar;
        switch (piece.getType()) {
            case PAWN:
                pieceChar = 'p';
                break;
            case KNIGHT:
                pieceChar = 'n';
                break;
            case BISHOP:
                pieceChar = 'b';
                break;
            case ROOK:
                pieceChar = 'r';
                break;
            case QUEEN:
                pieceChar = 'q';
                break;
            case KING:
                pieceChar = 'k';
                break;
            default:
                pieceChar = '.';
                break;
        }

        return piece.getColor() == Color.WHITE ? Character.toUpperCase(pieceChar) : pieceChar;
    }
}
