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

import demo.chess.api.dto.GameSnapshotDto;
import demo.chess.api.dto.UciGameDto;
import demo.chess.api.dto.UciGameMoveDto;
import demo.chess.definitions.Color;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.states.State;
import demo.chess.game.Game;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;
import demo.chess.notation.PgnNotation;
import demo.chess.save.GameSaver;

@Service
public class UciGameService {

    private static final DateTimeFormatter PGN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final GameService gameService;
    private final EngineRuntimeSelectionService engineRuntimeSelectionService;
    private final GameLoader gameLoader = new GameLoader();
    private final GameSaver gameSaver = new GameSaver();

    /**
     * Optional analysis-only game loaded from a PGN file. It deliberately does not
     * replace GameService's live game and therefore never starts the live clocks.
     */
    private Game importedAnalysisGame;
    private Map<String, String> importedPgnTags = new LinkedHashMap<>();

    public UciGameService(
            GameService gameService,
            EngineRuntimeSelectionService engineRuntimeSelectionService) {
        this.gameService = gameService;
        this.engineRuntimeSelectionService = engineRuntimeSelectionService;
    }

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
                BoardPositionSerializer.toPositionString(importedGame),
                moveDtos,
                playerName(pgnTags.get("White"), "White"),
                playerName(pgnTags.get("Black"), "Black"));
    }

    public synchronized String exportGame(boolean whiteComputerControlled, boolean blackComputerControlled)
            throws NoMoveFoundException, IOException {
        return gameSaver.toPgn(
                getAnalysisMoveListSnapshot(),
                getPgnTagsForExport(whiteComputerControlled, blackComputerControlled));
    }

    public synchronized List<Move> getAnalysisMoveListSnapshot() {
        if (importedAnalysisGame != null) {
            return new ArrayList<>(importedAnalysisGame.getMoveList());
        }
        return gameService.getMoveListSnapshot();
    }

    public synchronized boolean hasImportedGame() {
        return importedAnalysisGame != null;
    }

    public synchronized void clearImportedGame() {
        importedAnalysisGame = null;
        importedPgnTags = new LinkedHashMap<>();
    }

    public synchronized GameSnapshotDto getCurrentGameSnapshot() throws NoMoveFoundException, IOException {
        boolean imported = importedAnalysisGame != null;
        Game sourceGame = imported ? importedAnalysisGame : gameService.getCurrentGame();

        if (sourceGame == null) {
            return new GameSnapshotDto(
                    imported,
                    new UciGameDto(0, null, "", List.of(), "White", "Black"));
        }

        List<Move> originalMoves = imported
                ? new ArrayList<>(importedAnalysisGame.getMoveList())
                : gameService.getMoveListSnapshot();
        List<UciGameMoveDto> moveDtos = createMoveDtos(originalMoves);
        String sideToMove = sourceGame.getPlayer() != null && sourceGame.getPlayer().getColor() != null
                ? sourceGame.getPlayer().getColor().name().toLowerCase(Locale.ROOT)
                : null;

        String whitePlayerName = imported
                ? playerName(importedPgnTags.get("White"), "White")
                : playerName(sourceGame.getWhitePlayer() != null ? sourceGame.getWhitePlayer().getName() : null, "White");
        String blackPlayerName = imported
                ? playerName(importedPgnTags.get("Black"), "Black")
                : playerName(sourceGame.getBlackPlayer() != null ? sourceGame.getBlackPlayer().getName() : null, "Black");

        return new GameSnapshotDto(
                imported,
                new UciGameDto(
                        originalMoves.size(),
                        sideToMove,
                        BoardPositionSerializer.toPositionString(sourceGame),
                        moveDtos,
                        whitePlayerName,
                        blackPlayerName));
    }

    /**
     * Replays the move list once. The same simulation now supplies both canonical
     * display notation and the board snapshot after each move.
     */
    private List<UciGameMoveDto> createMoveDtos(List<Move> originalMoves)
            throws NoMoveFoundException, IOException {
        List<UciGameMoveDto> result = new ArrayList<>();
        Simulation replayGame = Simulation.createSimulation();

        int ply = 0;
        for (Move originalMove : originalMoves) {
            ply++;

            Move replayMove = replayGame.getPlayer().getMoveInSimulation(replayGame, originalMove);
            if (replayMove == null) {
                throw new NoMoveFoundException("Could not map replay move: " + originalMove);
            }

            String san = PgnNotation.toDisplayNotationAndApply(replayGame, replayMove);

            result.add(new UciGameMoveDto(
                    ply,
                    originalMove.toString(),
                    san,
                    BoardPositionSerializer.toPositionString(replayGame)));
        }

        return result;
    }

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
            gamePlayerName = color == Color.WHITE
                    ? game.getWhitePlayer().getName()
                    : game.getBlackPlayer().getName();
        }
        return playerName(gamePlayerName, fallback);
    }

    private String playerName(String name, String fallback) {
        if (name == null || name.isBlank()
                || "ChessGame".equals(name)
                || "Simulation".equals(name)) {
            return fallback;
        }
        return name;
    }

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

    

    
}
