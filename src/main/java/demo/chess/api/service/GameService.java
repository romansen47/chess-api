package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import demo.chess.admin.impl.ChessAdmin;
import demo.chess.api.dto.BoardDto;
import demo.chess.api.dto.GameSettingsDto;
import demo.chess.api.mapper.BoardDtoMapper;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;

/**
 * Owns the lifecycle of the currently played game.
 *
 * <p>The service is intentionally an application coordinator: DTO validation
 * is delegated to {@link GameSettingsPolicy}, board projection to
 * {@link BoardDtoMapper}, and chess rules remain in the core module.</p>
 */
@Service
public class GameService {

    public static final int DEFAULT_TIME_SECONDS = 5 * 60;
    public static final int DEFAULT_INCREMENT_SECONDS = 0;

    private Game game;
    private GameSettingsDto gameSettings;

    public GameService() {
        this.gameSettings = GameSettingsPolicy.defaults();
        this.game = createGame(this.gameSettings);
    }

    /** Starts a new game with the currently configured settings. */
    public synchronized GameSettingsDto startNewGame() {
        return startNewGame(this.gameSettings);
    }

    /**
     * Validates the request settings and replaces the current game atomically.
     *
     * @param settings requested settings; {@code null} reuses current settings
     * @return normalized settings snapshot
     */
    public synchronized GameSettingsDto startNewGame(GameSettingsDto settings) {
        GameSettingsDto normalizedSettings = GameSettingsPolicy.normalize(settings, this.gameSettings);
        this.gameSettings = normalizedSettings;
        this.game = createGame(normalizedSettings);
        return GameSettingsPolicy.copy(this.gameSettings);
    }

    /** Returns a defensive copy of the current game settings. */
    public synchronized GameSettingsDto getGameSettings() {
        return GameSettingsPolicy.copy(this.gameSettings);
    }

    /** Returns the current core game instance. */
    public synchronized Game getCurrentGame() {
        return game;
    }

    private Game createGame(GameSettingsDto settings) {
        ChessStartingPosition startingPosition = ChessStartingPosition.of(settings.getStartingPositionId());
        Game createdGame = new ChessAdmin().chessGame(
                settings.getTimeForEachPlayerSeconds(),
                startingPosition);
        createdGame.configureTimeControl(
                settings.getIncrementForWhiteSeconds(),
                settings.getIncrementForBlackSeconds(),
                settings.getAdditionalTimeAfter40MovesSeconds());
        return createdGame;
    }

    /** Applies an already resolved legal move to the current game. */
    public synchronized Move applyMove(Move move) throws NoMoveFoundException, IOException {
        if (move == null) throw new NoMoveFoundException("move must not be null");
        game.apply(move);
        return move;
    }

    /** Applies a move only while the caller still refers to the current game instance. */
    public synchronized boolean applyMoveIfCurrent(Game expectedGame, Move move)
            throws NoMoveFoundException, IOException {
        if (expectedGame == null || expectedGame != this.game) return false;
        applyMove(move);
        return true;
    }

    /** Resolves API coordinates through the core legal-move resolver and applies the move. */
    public synchronized Move applyMove(String from, String to, String promotion)
            throws NoMoveFoundException, IOException {
        Move selected = LegalMoveResolver.resolveCoordinates(game, from, to, promotion);
        return applyMove(selected);
    }

    /** Returns the compact serialized representation of the current position. */
    public synchronized String getCurrentPositionString() {
        return BoardPositionSerializer.toPositionString(game);
    }

    /** Returns a shallow move snapshot suitable for read-only application use. */
    public synchronized List<Move> getMoveListSnapshot() {
        return new ArrayList<>(game.getMoveList());
    }

    /** Returns the current board as an API DTO. */
    public synchronized BoardDto getBoardView() {
        return BoardDtoMapper.toDto(game);
    }

    /** Maps an arbitrary game board to the API DTO representation. */
    public BoardDto getBoardView(Game sourceGame) {
        return BoardDtoMapper.toDto(sourceGame);
    }
}
