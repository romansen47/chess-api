package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import demo.chess.admin.impl.ChessAdmin;
import demo.chess.api.dto.BoardDto;
import demo.chess.api.dto.GameSettingsDto;
import demo.chess.api.dto.PieceDto;
import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.states.State;
import demo.chess.game.Game;

@Service
public class GameService {

    /**
     * Standard-Bedenkzeit für eine neue Partie: 5 Minuten = 5 * 60 Sekunden.
     */
    public static final int DEFAULT_TIME_SECONDS = 5 * 60;

    /**
     * Standard-Inkrement: 0 Sekunden, also 5+0.
     */
    public static final int DEFAULT_INCREMENT_SECONDS = 0;

    /**
     * Aktuelles Spiel. Für den Moment arbeiten wir mit genau einer Partie,
     * die beim Start der Anwendung erzeugt wird.
     */
    private Game game;

    /**
     * Aktuelle Einstellungen, die für neue Partien verwendet werden.
     */
    private GameSettingsDto gameSettings;

    /**
     * Creates a new GameService instance.
     */
    public GameService() {
        this.gameSettings = createDefaultGameSettings();
        this.game = createGame(
                this.gameSettings.getTimeForEachPlayerSeconds(),
                this.gameSettings.getIncrementForWhiteSeconds(),
                this.gameSettings.getIncrementForBlackSeconds());
    }

    /**
     * Starts the new game.
     * @return the result of the operation
     */
    public synchronized GameSettingsDto startNewGame() {
        return startNewGame(this.gameSettings);
    }

    /**
     * Starts the new game.
     * @param settings the settings
     * @return the result of the operation
     */
    public synchronized GameSettingsDto startNewGame(GameSettingsDto settings) {
        GameSettingsDto normalizedSettings = normalizeGameSettings(settings);

        this.gameSettings = normalizedSettings;
        this.game = createGame(
                normalizedSettings.getTimeForEachPlayerSeconds(),
                normalizedSettings.getIncrementForWhiteSeconds(),
                normalizedSettings.getIncrementForBlackSeconds());

        return copyGameSettings(this.gameSettings);
    }

    /**
     * Returns the game settings.
     * @return the game settings
     */
    public synchronized GameSettingsDto getGameSettings() {
        return copyGameSettings(this.gameSettings);
    }

    /**
     * Returns the current game.
     * @return the current game
     */
    public synchronized Game getCurrentGame() {
        return game;
    }

    /**
     * Creates the default game settings.
     * @return the result of the operation
     */
    private GameSettingsDto createDefaultGameSettings() {
        return new GameSettingsDto(
                DEFAULT_TIME_SECONDS,
                DEFAULT_INCREMENT_SECONDS,
                DEFAULT_INCREMENT_SECONDS,
                0,
                "WHITE",
                0);
    }

    /**
     * Performs the normalize game settings operation.
     * @param settings the settings
     * @return the result of the operation
     */
    private GameSettingsDto normalizeGameSettings(GameSettingsDto settings) {
        GameSettingsDto source = settings != null ? settings : this.gameSettings;
        if (source == null) {
            source = createDefaultGameSettings();
        }

        int timeForEachPlayerSeconds = source.getTimeForEachPlayerSeconds() > 0
                ? source.getTimeForEachPlayerSeconds()
                : DEFAULT_TIME_SECONDS;
        int incrementForWhiteSeconds = Math.max(0, source.getIncrementForWhiteSeconds());
        int incrementForBlackSeconds = Math.max(0, source.getIncrementForBlackSeconds());
        int additionalTimeAfter40MovesSeconds = Math.max(0, source.getAdditionalTimeAfter40MovesSeconds());
        String startingColor = source.getStartingColor() != null && !source.getStartingColor().isBlank()
                ? source.getStartingColor().trim().toUpperCase(Locale.ROOT)
                : "WHITE";

        long nextVersion = this.gameSettings != null ? this.gameSettings.getVersion() + 1 : 1;

        return new GameSettingsDto(
                timeForEachPlayerSeconds,
                incrementForWhiteSeconds,
                incrementForBlackSeconds,
                additionalTimeAfter40MovesSeconds,
                startingColor,
                nextVersion);
    }

    /**
     * Performs the copy game settings operation.
     * @param settings the settings
     * @return the result of the operation
     */
    private GameSettingsDto copyGameSettings(GameSettingsDto settings) {
        if (settings == null) {
            return createDefaultGameSettings();
        }

        return new GameSettingsDto(
                settings.getTimeForEachPlayerSeconds(),
                settings.getIncrementForWhiteSeconds(),
                settings.getIncrementForBlackSeconds(),
                settings.getAdditionalTimeAfter40MovesSeconds(),
                settings.getStartingColor(),
                settings.getVersion());
    }

    /**
     * Creates the game.
     * @param timeSeconds the time seconds
     * @param whiteIncrementSeconds the white increment seconds
     * @param blackIncrementSeconds the black increment seconds
     * @return the result of the operation
     */
    private Game createGame(int timeSeconds, int whiteIncrementSeconds, int blackIncrementSeconds) {
        Game createdGame = new ChessAdmin().chessGame(timeSeconds);

        createdGame.setIncrementForWhite(whiteIncrementSeconds);
        createdGame.setIncrementForBlack(blackIncrementSeconds);

        setupClocks(createdGame, whiteIncrementSeconds, blackIncrementSeconds);

        return createdGame;
    }

    /**
     * Sets the up clocks.
     * @param chessGame the chess game
     * @param whiteIncrementSeconds the white increment seconds
     * @param blackIncrementSeconds the black increment seconds
     */
    private void setupClocks(Game chessGame, int whiteIncrementSeconds, int blackIncrementSeconds) {
        chessGame.getWhitePlayer().setupClock(
                chessGame.getTimeForEachPlayer(),
                whiteIncrementSeconds,
                () -> {
                    chessGame.setState(State.LOST_ON_TIME);
                    stopClocks(chessGame);
                    System.out.println("[GameService] White lost on time.");
                });

        chessGame.getBlackPlayer().setupClock(
                chessGame.getTimeForEachPlayer(),
                blackIncrementSeconds,
                () -> {
                    chessGame.setState(State.LOST_ON_TIME);
                    stopClocks(chessGame);
                    System.out.println("[GameService] Black lost on time.");
                });
    }

    /**
     * Stops the clocks.
     * @param chessGame the chess game
     */
    private void stopClocks(Game chessGame) {
        if (chessGame.getWhitePlayer().getChessClock().isStarted()
                && !chessGame.getWhitePlayer().getChessClock().isStopped()) {
            chessGame.getWhitePlayer().getChessClock().stop();
        }

        if (chessGame.getBlackPlayer().getChessClock().isStarted()
                && !chessGame.getBlackPlayer().getChessClock().isStopped()) {
            chessGame.getBlackPlayer().getChessClock().stop();
        }
    }

    /**
     * Applies the move.
     * @param move the move
     * @return the result of the operation
     */
    public synchronized Move applyMove(Move move) throws NoMoveFoundException, IOException {
        if (move == null) {
            throw new NoMoveFoundException("move must not be null");
        }
        game.apply(move);
        return move;
    }


    /**
     * Applies the move if current.
     * @param expectedGame the expected game
     * @param move the move
     * @return the result of the operation
     */
    public synchronized boolean applyMoveIfCurrent(Game expectedGame, Move move) throws NoMoveFoundException, IOException {
        if (expectedGame == null || expectedGame != this.game) {
            return false;
        }
        applyMove(move);
        return true;
    }

    /**
     * Applies the move.
     * @param from the from
     * @param to the to
     * @param promotion the promotion
     * @return the result of the operation
     */
    public synchronized Move applyMove(String from, String to, String promotion)
            throws NoMoveFoundException, IOException {

        if (from == null || to == null) {
            throw new NoMoveFoundException("from/to must not be null");
        }

        String fromNorm = from.toLowerCase(Locale.ROOT);
        String toNorm = to.toLowerCase(Locale.ROOT);

        List<Move> legalMoves = game.getPlayer().getValidMoves(game);
        List<Move> matchingMoves = new ArrayList<>();

        for (Move m : legalMoves) {
            if (m.getSource() == null || m.getTarget() == null) {
                continue;
            }

            String srcName = m.getSource().getName();
            String tgtName = m.getTarget().getName();
            if (srcName == null || tgtName == null) {
                continue;
            }

            if (srcName.equalsIgnoreCase(fromNorm) && tgtName.equalsIgnoreCase(toNorm)) {
                matchingMoves.add(m);
            }
        }

        if (matchingMoves.isEmpty()) {
            throw new NoMoveFoundException("No legal move for " + from + " -> " + to);
        }

        Move selected = selectMoveForPromotion(matchingMoves, promotion, from, to);
        return applyMove(selected);
    }

    /**
     * Performs the select move for promotion operation.
     * @param matchingMoves the matching moves
     * @param promotion the promotion
     * @param from the from
     * @param to the to
     * @return the result of the operation
     */
    private Move selectMoveForPromotion(List<Move> matchingMoves, String promotion, String from, String to)
            throws NoMoveFoundException {

        String promotionLabel = normalizePromotion(promotion);

        if (promotionLabel == null) {
            return matchingMoves.get(0);
        }

        for (Move move : matchingMoves) {
            if (!(move instanceof Promotion)) {
                continue;
            }

            Promotion promotionMove = (Promotion) move;
            if (promotionMove.getPromotedPiece() == null
                    || promotionMove.getPromotedPiece().getType() == null) {
                continue;
            }

            if (promotionLabel.equals(promotionMove.getPromotedPiece().getType().label)) {
                return move;
            }
        }

        throw new NoMoveFoundException(
                "No legal promotion move for " + from + " -> " + to + " with promotion " + promotion);
    }

    /**
     * Performs the normalize promotion operation.
     * @param promotion the promotion
     * @return the result of the operation
     */
    private String normalizePromotion(String promotion) {
        if (promotion == null || promotion.isBlank()) {
            return null;
        }

        String normalized = promotion.trim().toLowerCase(Locale.ROOT);

        switch (normalized) {
            case "q":
            case "queen":
                return "q";
            case "r":
            case "rook":
                return "r";
            case "b":
            case "bishop":
                return "b";
            case "n":
            case "knight":
                return "n";
            default:
                return normalized;
        }
    }

    /**
     * Returns the current position string.
     * @return the current position string
     */
    public synchronized String getCurrentPositionString() {
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
                pieceChar = '?';
                break;
        }

        return piece.getColor() == Color.WHITE
                ? Character.toUpperCase(pieceChar)
                : pieceChar;
    }

    /**
     * Returns the move list snapshot.
     * @return the move list snapshot
     */
    public synchronized List<Move> getMoveListSnapshot() {
        return new ArrayList<>(game.getMoveList());
    }

    /**
     * Returns the board view.
     * @return the board view
     */
    public synchronized BoardDto getBoardView() {
        return getBoardView(game);
    }

    /**
     * Returns the board view.
     * @param sourceGame the source game
     * @return the board view
     */
    public BoardDto getBoardView(Game sourceGame) {
        Board board = sourceGame.getChessBoard();
        List<PieceDto> pieces = new ArrayList<>();

        for (int file = 1; file <= 8; file++) {
            for (int rank = 1; rank <= 8; rank++) {
                Field field = board.getField(file, rank);
                if (field == null) {
                    continue;
                }

                Piece piece = field.getPiece();
                if (piece == null) {
                    continue;
                }

                String square = field.getName();

                Color color = piece.getColor();
                PieceType type = piece.getType();

                String colorStr = (color != null)
                        ? color.name().toLowerCase(Locale.ROOT)
                        : "unknown";

                String typeStr = (type != null)
                        ? type.name().toLowerCase(Locale.ROOT)
                        : "piece";

                pieces.add(new PieceDto(colorStr, typeStr, square));
            }
        }

        return new BoardDto(pieces);
    }
}