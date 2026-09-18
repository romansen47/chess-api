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

import demo.chess.api.dto.GameAnnotationDto;
import demo.chess.api.dto.GameSnapshotDto;
import demo.chess.api.dto.UciGameDto;
import demo.chess.api.dto.UciGameMoveDto;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.Color;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.MoveList;
import demo.chess.definitions.moves.impl.MoveListImpl;
import demo.chess.definitions.states.State;
import demo.chess.game.Game;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;
import demo.chess.notation.PgnAnnotationParser;
import demo.chess.notation.PgnMoveAnnotation;
import demo.chess.save.GameSaver;

/**
 * Owns import/export and snapshot state for the game selected for analysis.
 *
 * <p>The service deliberately distinguishes the live game from an imported
 * analysis game. Imported PGN metadata is kept in one
 * {@link ImportedGameContext}; per-ply UCI/SAN reconstruction is delegated to
 * {@link UciGameMoveMapper}. Every replay snapshot carries the Chess960
 * starting-position id and initial FEN.</p>
 */
@Service
public class UciGameService {

    private static final DateTimeFormatter PGN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final GameService gameService;
    private final EngineRuntimeSelectionService engineRuntimeSelectionService;
    private final GameLoader gameLoader = new GameLoader();
    private final GameSaver gameSaver = new GameSaver();
    private final PgnAnnotationParser annotationParser = new PgnAnnotationParser();

    private ImportedGameContext importedContext;
    private Map<Integer, PgnMoveAnnotation> liveAnnotations = new LinkedHashMap<>();

    public UciGameService(GameService gameService, EngineRuntimeSelectionService engineRuntimeSelectionService) {
        this.gameService = gameService;
        this.engineRuntimeSelectionService = engineRuntimeSelectionService;
    }

    /** Imports a PGN as the currently selected analysis game. */
    public synchronized UciGameDto importGame(String content) throws NoMoveFoundException, IOException {
        return importGame(content, null);
    }

    /**
     * Imports a PGN as the selected analysis game and optionally associates a
     * database game id with it.
     */
    public synchronized UciGameDto importGame(String content, Long databaseGameId)
            throws NoMoveFoundException, IOException {
        ChessStartingPosition startingPosition = gameLoader.parsePgnStartingPosition(content);
        List<String> uciMoves = gameLoader.parsePgnMoveList(content);

        Simulation importedGame = Simulation.createSimulation(startingPosition);
        gameLoader.loadGame(uciMoves, importedGame);

        Map<String, String> pgnTags = new LinkedHashMap<>(gameLoader.parsePgnTags(content));
        Map<Integer, PgnMoveAnnotation> annotations = new LinkedHashMap<>(annotationParser.parse(content));
        this.importedContext = new ImportedGameContext(
                importedGame,
                pgnTags,
                databaseGameId,
                annotations);
        this.liveAnnotations = new LinkedHashMap<>();
        return createGameDto(importedGame, pgnTags, databaseGameId, annotations, true);
    }

    /** Exports the selected game as PGN, preserving imported tags and annotations. */
    public synchronized String exportGame(boolean whiteComputerControlled, boolean blackComputerControlled)
            throws NoMoveFoundException, IOException {
        return gameSaver.toPgn(
                getAnalysisMoveHistorySnapshot(),
                getPgnTagsForExport(whiteComputerControlled, blackComputerControlled),
                currentAnnotations());
    }

    /**
     * Returns a defensive snapshot of the selected game's moves.
     *
     * <p>The public return type intentionally remains {@link List} for
     * compatibility with existing consumers. Internal replay/export paths use
     * {@link #getAnalysisMoveHistorySnapshot()} so the Chess960 starting
     * position remains attached to the history.</p>
     */
    public synchronized List<Move> getAnalysisMoveListSnapshot() {
        return new ArrayList<>(getAnalysisGameContext().moves());
    }

    /** Returns the start position for the selected live/imported analysis game. */
    public synchronized ChessStartingPosition getAnalysisStartingPosition() {
        return getAnalysisGameContext().startingPosition();
    }

    /**
     * Returns start position and move history as one atomic analysis snapshot.
     *
     * <p>Consumers that reconstruct historical positions must use this context
     * instead of reading the move list and silently assuming classical position
     * 518. Keeping both values under the same synchronized snapshot also
     * prevents a selected-game change from mixing two different contexts.</p>
     */
    synchronized AnalysisGameContext getAnalysisGameContext() {
        Game source = selectedGame();
        if (source == null) {
            return new AnalysisGameContext(ChessStartingPosition.STANDARD, List.of());
        }
        ChessStartingPosition startingPosition = source.getStartingPosition() != null
                ? source.getStartingPosition()
                : ChessStartingPosition.STANDARD;
        return new AnalysisGameContext(
                startingPosition,
                new ArrayList<>(source.getMoveList()));
    }

    public synchronized boolean hasImportedGame() {
        return importedContext != null;
    }

    /** Clears the imported selection and returns analysis to the current live game. */
    public synchronized void clearImportedGame() {
        importedContext = null;
        liveAnnotations = new LinkedHashMap<>();
    }

    public synchronized Long getImportedDatabaseGameId() {
        return importedContext != null ? importedContext.databaseGameId() : null;
    }

    public synchronized void setImportedDatabaseGameId(Long databaseGameId) {
        if (importedContext != null) importedContext.setDatabaseGameId(databaseGameId);
    }

    public synchronized List<GameAnnotationDto> getAnnotationDtos() {
        return annotationDtos(currentAnnotations());
    }

    /** Replaces annotations and returns the resulting PGN representation. */
    public synchronized String updateAnnotations(
            List<GameAnnotationDto> annotations,
            boolean whiteComputerControlled,
            boolean blackComputerControlled)
            throws NoMoveFoundException, IOException {
        Map<Integer, PgnMoveAnnotation> updated = new LinkedHashMap<>();
        if (annotations != null) {
            for (GameAnnotationDto annotation : annotations) {
                if (annotation == null || annotation.ply() <= 0) continue;
                PgnMoveAnnotation value = new PgnMoveAnnotation(
                        annotation.nag(), annotation.comment(), annotation.evaluation(), annotation.variations());
                if (!value.isEmpty()) updated.put(annotation.ply(), value);
            }
        }
        String pgn = gameSaver.toPgn(
                getAnalysisMoveHistorySnapshot(),
                getPgnTagsForExport(whiteComputerControlled, blackComputerControlled),
                updated);
        if (importedContext != null) importedContext.setAnnotations(updated);
        else liveAnnotations = new LinkedHashMap<>(updated);
        return pgn;
    }

    /** Returns a complete frontend snapshot of the currently selected game. */
    public synchronized GameSnapshotDto getCurrentGameSnapshot() throws NoMoveFoundException, IOException {
        boolean imported = importedContext != null;
        Game sourceGame = selectedGame();
        if (sourceGame == null) {
            return new GameSnapshotDto(imported,
                    new UciGameDto(0, null, "", List.of(), "White", "Black"));
        }

        Map<String, String> tags = imported ? importedContext.pgnTagsCopy() : Map.of();
        Long databaseGameId = imported ? importedContext.databaseGameId() : null;
        return new GameSnapshotDto(
                imported,
                createGameDto(sourceGame, tags, databaseGameId, currentAnnotations(), imported));
    }

    private UciGameDto createGameDto(
            Game sourceGame,
            Map<String, String> pgnTags,
            Long databaseGameId,
            Map<Integer, PgnMoveAnnotation> annotations,
            boolean imported)
            throws NoMoveFoundException, IOException {
        MoveList moves = copyMoveList(sourceGame);
        List<UciGameMoveDto> moveDtos = UciGameMoveMapper.toDtos(moves);
        String whitePlayerName = imported
                ? playerName(pgnTags.get("White"), "White")
                : playerName(sourceGame.getWhitePlayer() != null ? sourceGame.getWhitePlayer().getName() : null, "White");
        String blackPlayerName = imported
                ? playerName(pgnTags.get("Black"), "Black")
                : playerName(sourceGame.getBlackPlayer() != null ? sourceGame.getBlackPlayer().getName() : null, "Black");
        ChessStartingPosition startingPosition = sourceGame.getStartingPosition() != null
                ? sourceGame.getStartingPosition()
                : ChessStartingPosition.STANDARD;

        return new UciGameDto(
                moves.size(),
                sideToMove(sourceGame),
                BoardPositionSerializer.toPositionString(sourceGame),
                moveDtos,
                whitePlayerName,
                blackPlayerName,
                databaseGameId,
                annotationDtos(annotations),
                startingPosition.getId(),
                startingPosition.initialFen());
    }

    private MoveList getAnalysisMoveHistorySnapshot() {
        return copyMoveList(selectedGame());
    }

    private MoveList copyMoveList(Game sourceGame) {
        MoveList copy = new MoveListImpl();
        if (sourceGame == null) return copy;
        copy.setStartingPosition(sourceGame.getStartingPosition());
        copy.addAll(sourceGame.getMoveList());
        return copy;
    }

    private Game selectedGame() {
        return importedContext != null ? importedContext.game() : gameService.getCurrentGame();
    }

    private Map<Integer, PgnMoveAnnotation> currentAnnotations() {
        return importedContext != null
                ? importedContext.annotationsCopy()
                : new LinkedHashMap<>(liveAnnotations);
    }

    private String sideToMove(Game game) {
        return game.getPlayer() != null && game.getPlayer().getColor() != null
                ? game.getPlayer().getColor().name().toLowerCase(Locale.ROOT)
                : null;
    }

    private List<GameAnnotationDto> annotationDtos(Map<Integer, PgnMoveAnnotation> annotations) {
        if (annotations == null || annotations.isEmpty()) return List.of();
        return annotations.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new GameAnnotationDto(
                        entry.getKey(),
                        entry.getValue().nag(),
                        entry.getValue().comment(),
                        entry.getValue().evaluation(),
                        entry.getValue().variations()))
                .toList();
    }

    private Map<String, String> getPgnTagsForExport(boolean whiteComputerControlled, boolean blackComputerControlled) {
        if (importedContext != null) return importedContext.pgnTagsCopy();
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

    private String playerNameForExport(Game game, Color color, boolean computerControlled) {
        String fallback = color == Color.WHITE ? "White" : "Black";
        if (computerControlled) {
            String engineName = color == Color.WHITE
                    ? engineRuntimeSelectionService.getWhitePlayerEngineName()
                    : engineRuntimeSelectionService.getBlackPlayerEngineName();
            return playerName(engineName, fallback + " Engine");
        }
        String gamePlayerName = null;
        if (game != null) {
            gamePlayerName = color == Color.WHITE ? game.getWhitePlayer().getName() : game.getBlackPlayer().getName();
        }
        return playerName(gamePlayerName, fallback);
    }

    private String playerName(String name, String fallback) {
        if (name == null || name.isBlank() || "ChessGame".equals(name) || "Simulation".equals(name)) return fallback;
        return name;
    }

    private String gameResult(Game game) {
        if (game == null || game.getState() == null) return "*";
        State state = game.getState();
        if (state == State.BLACK_MATED || state == State.BLACK_RESIGNED) return "1-0";
        if (state == State.WHITE_MATED || state == State.WHITE_RESIGNED) return "0-1";
        if (state == State.STALEMATE
                || state == State.DRAW_BY_50_MOVES_RULE
                || state == State.DRAW_BY_THREEFOLD_REPETITION
                || state == State.DRAW_BY_INSUFFICIENT_MATERIAL) return "1/2-1/2";
        if (state == State.LOST_ON_TIME && game.getTimedOutColor() != null) {
            return game.getTimedOutColor() == Color.WHITE ? "0-1" : "1-0";
        }
        return "*";
    }
}
