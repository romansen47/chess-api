package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import demo.chess.admin.impl.ChessAdmin;
import demo.chess.api.dto.UciGameDto;
import demo.chess.api.dto.UciGameMoveDto;
import demo.chess.definitions.Color;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.players.Player;
import demo.chess.game.Game;
import demo.chess.game.impl.ChessGame;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;
import demo.chess.save.GameSaver;

@Service
public class UciGameService {

    private final GameService gameService;
    private final GameLoader gameLoader = new GameLoader();
    private final GameSaver gameSaver = new GameSaver();

    /**
     * Optional analysis-only game loaded from a UCI move file. It deliberately does
     * not replace GameService's live game and therefore never starts the live clocks.
     */
    private Game importedAnalysisGame;

    public UciGameService(GameService gameService) {
        this.gameService = gameService;
    }

    public synchronized UciGameDto importGame(String content) throws NoMoveFoundException, IOException {
        List<String> uciMoves = gameLoader.parseMoveList(content);

        Simulation importedGame = Simulation.createSimulation();
        gameLoader.loadGame(uciMoves, importedGame);

        List<UciGameMoveDto> moveDtos = createMoveDtos(importedGame.getMoveList());
        this.importedAnalysisGame = importedGame;

        String sideToMove = importedGame.getPlayer() != null && importedGame.getPlayer().getColor() != null
                ? importedGame.getPlayer().getColor().name().toLowerCase(Locale.ROOT)
                : null;

        return new UciGameDto(
                importedGame.getMoveList().size(),
                sideToMove,
                toPositionString(importedGame),
                moveDtos);
    }

    public synchronized String exportGame() {
        return gameSaver.toUci(getAnalysisMoveListSnapshot());
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
    }

    private List<UciGameMoveDto> createMoveDtos(List<Move> originalMoves)
            throws NoMoveFoundException, IOException {
        List<UciGameMoveDto> result = new ArrayList<>();
        Simulation replayGame = Simulation.createSimulation();
        ChessGame notationGame = (ChessGame) new ChessAdmin().chessGame(GameService.DEFAULT_TIME_SECONDS);
        notationGame.getWhitePlayer().setupClock(GameService.DEFAULT_TIME_SECONDS, 0, () -> { });
        notationGame.getBlackPlayer().setupClock(GameService.DEFAULT_TIME_SECONDS, 0, () -> { });

        try {
            int ply = 0;
            for (Move originalMove : originalMoves) {
                ply++;

                Move replayMove = replayGame.getPlayer().getMoveInSimulation(replayGame, originalMove);
                Move notationMove = notationGame.getPlayer().getMoveInSimulation(notationGame, originalMove);
                String san = notationGame.getShortAlgebraicNotatedMove(notationMove);

                replayGame.apply(replayMove);
                notationGame.apply(notationMove);

                result.add(new UciGameMoveDto(
                        ply,
                        originalMove.toString(),
                        san,
                        toPositionString(replayGame)));
            }
        } finally {
            stopClock(notationGame.getWhitePlayer());
            stopClock(notationGame.getBlackPlayer());
        }

        return result;
    }

    private void stopClock(Player player) {
        if (player != null
                && player.getChessClock() != null
                && player.getChessClock().isStarted()
                && !player.getChessClock().isStopped()) {
            player.getChessClock().stop();
        }
    }

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
