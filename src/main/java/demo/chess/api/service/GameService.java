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
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.pieces.Piece;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;

@Service
public class GameService {

    public static final int DEFAULT_TIME_SECONDS = 5 * 60;
    public static final int DEFAULT_INCREMENT_SECONDS = 0;

    private Game game;
    private GameSettingsDto gameSettings;

    public GameService() {
        this.gameSettings = createDefaultGameSettings();
        this.game = createGame(this.gameSettings);
    }

    public synchronized GameSettingsDto startNewGame() {
        return startNewGame(this.gameSettings);
    }

    public synchronized GameSettingsDto startNewGame(GameSettingsDto settings) {
        GameSettingsDto normalizedSettings = normalizeGameSettings(settings);
        this.gameSettings = normalizedSettings;
        this.game = createGame(normalizedSettings);
        return copyGameSettings(this.gameSettings);
    }

    public synchronized GameSettingsDto getGameSettings() {
        return copyGameSettings(this.gameSettings);
    }

    public synchronized Game getCurrentGame() {
        return game;
    }

    private GameSettingsDto createDefaultGameSettings() {
        return new GameSettingsDto(
                DEFAULT_TIME_SECONDS,
                DEFAULT_INCREMENT_SECONDS,
                DEFAULT_INCREMENT_SECONDS,
                0,
                "WHITE",
                ChessStartingPosition.STANDARD_ID,
                0);
    }

    private GameSettingsDto normalizeGameSettings(GameSettingsDto settings) {
        GameSettingsDto source = settings != null ? settings : this.gameSettings;
        if (source == null) source = createDefaultGameSettings();

        int timeForEachPlayerSeconds = source.getTimeForEachPlayerSeconds() > 0
                ? source.getTimeForEachPlayerSeconds()
                : DEFAULT_TIME_SECONDS;
        int incrementForWhiteSeconds = Math.max(0, source.getIncrementForWhiteSeconds());
        int incrementForBlackSeconds = Math.max(0, source.getIncrementForBlackSeconds());
        int additionalTimeAfter40MovesSeconds = Math.max(0, source.getAdditionalTimeAfter40MovesSeconds());
        String startingColor = source.getStartingColor() != null && !source.getStartingColor().isBlank()
                ? source.getStartingColor().trim().toUpperCase(Locale.ROOT)
                : "WHITE";
        int startingPositionId = source.getStartingPositionId();
        if (startingPositionId < ChessStartingPosition.MIN_ID || startingPositionId > ChessStartingPosition.MAX_ID) {
            throw new IllegalArgumentException("Chess960 startingPositionId must be between 0 and 959");
        }
        long nextVersion = this.gameSettings != null ? this.gameSettings.getVersion() + 1 : 1;

        return new GameSettingsDto(
                timeForEachPlayerSeconds,
                incrementForWhiteSeconds,
                incrementForBlackSeconds,
                additionalTimeAfter40MovesSeconds,
                startingColor,
                startingPositionId,
                nextVersion);
    }

    private GameSettingsDto copyGameSettings(GameSettingsDto settings) {
        if (settings == null) return createDefaultGameSettings();
        return new GameSettingsDto(
                settings.getTimeForEachPlayerSeconds(),
                settings.getIncrementForWhiteSeconds(),
                settings.getIncrementForBlackSeconds(),
                settings.getAdditionalTimeAfter40MovesSeconds(),
                settings.getStartingColor(),
                settings.getStartingPositionId(),
                settings.getVersion());
    }

    private Game createGame(GameSettingsDto settings) {
        Game createdGame = new ChessAdmin().chessGame(
                settings.getTimeForEachPlayerSeconds(),
                ChessStartingPosition.of(settings.getStartingPositionId()));
        createdGame.configureTimeControl(
                settings.getIncrementForWhiteSeconds(),
                settings.getIncrementForBlackSeconds(),
                settings.getAdditionalTimeAfter40MovesSeconds());
        return createdGame;
    }

    public synchronized Move applyMove(Move move) throws NoMoveFoundException, IOException {
        if (move == null) throw new NoMoveFoundException("move must not be null");
        game.apply(move);
        return move;
    }

    public synchronized boolean applyMoveIfCurrent(Game expectedGame, Move move)
            throws NoMoveFoundException, IOException {
        if (expectedGame == null || expectedGame != this.game) return false;
        applyMove(move);
        return true;
    }

    public synchronized Move applyMove(String from, String to, String promotion)
            throws NoMoveFoundException, IOException {
        Move selected = LegalMoveResolver.resolveCoordinates(game, from, to, promotion);
        return applyMove(selected);
    }

    public synchronized String getCurrentPositionString() {
        return BoardPositionSerializer.toPositionString(game);
    }

    public synchronized List<Move> getMoveListSnapshot() {
        return new ArrayList<>(game.getMoveList());
    }

    public synchronized BoardDto getBoardView() {
        return getBoardView(game);
    }

    public BoardDto getBoardView(Game sourceGame) {
        Board board = sourceGame.getChessBoard();
        List<PieceDto> pieces = new ArrayList<>();
        for (int file = 1; file <= 8; file++) {
            for (int rank = 1; rank <= 8; rank++) {
                Field field = board.getField(file, rank);
                if (field == null || field.getPiece() == null) continue;
                Piece piece = field.getPiece();
                String colorStr = piece.getColor() != null
                        ? piece.getColor().name().toLowerCase(Locale.ROOT)
                        : "unknown";
                String typeStr = piece.getType() != null
                        ? piece.getType().name().toLowerCase(Locale.ROOT)
                        : "piece";
                pieces.add(new PieceDto(colorStr, typeStr, field.getName()));
            }
        }
        return new BoardDto(pieces);
    }
}
