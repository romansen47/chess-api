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
import demo.chess.load.GameLoader;

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

    private final GameLoader gameLoader = new GameLoader();
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
        return startNewGame(settings, null, List.of());
    }

    /**
     * Starts a live game from an optional prepared opening history.
     *
     * <p>The opening is applied through the normal legal move path. Afterwards
     * only live clock/timing state is reset, so the full chess history remains
     * authoritative for castling, en passant, repetition and PGN export.</p>
     */
    public synchronized GameSettingsDto startNewGame(
            GameSettingsDto settings,
            ChessStartingPosition openingStartingPosition,
            List<String> openingMoves) {
        GameSettingsDto normalizedSettings = GameSettingsPolicy.normalize(settings, this.gameSettings);
        if (openingStartingPosition != null) {
            normalizedSettings.setStartingPositionId(openingStartingPosition.getId());
        }

        Game candidate = createGame(normalizedSettings);
        List<String> moves = openingMoves == null ? List.of() : List.copyOf(openingMoves);
        if (!moves.isEmpty()) {
            try {
                gameLoader.loadGame(moves, candidate);
                if (candidate.getState() != null) {
                    candidate.resetClocks();
                    throw new IllegalArgumentException(
                            "Selected opening ends in a finished game: " + candidate.getState());
                }
                candidate.resetClocks();
            } catch (NoMoveFoundException | IOException e) {
                stopCandidateClocks(candidate);
                throw new IllegalArgumentException(
                        "Could not apply selected opening: " + e.getMessage(), e);
            }
        }

        this.gameSettings = normalizedSettings;
        this.game = candidate;
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

    private void stopCandidateClocks(Game candidate) {
        if (candidate == null) return;
        if (candidate.getWhitePlayer() != null && candidate.getWhitePlayer().getChessClock() != null) {
            candidate.getWhitePlayer().getChessClock().stop();
        }
        if (candidate.getBlackPlayer() != null && candidate.getBlackPlayer().getChessClock() != null) {
            candidate.getBlackPlayer().getChessClock().stop();
        }
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
