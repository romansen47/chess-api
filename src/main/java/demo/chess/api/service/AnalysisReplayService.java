package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.AnalysisProfilePointDto;
import demo.chess.api.dto.AnalysisReplaySettingsDto;
import demo.chess.api.dto.AnalysisReplayStepDto;
import demo.chess.api.dto.BoardDto;
import demo.chess.api.dto.EngineLineDto;
import demo.chess.definitions.board.Board;
import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.engines.DeepAnalysisEngine;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.impl.DeepAnalysisUciEngine;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.pieces.Piece;
import demo.chess.definitions.players.Player;
import demo.chess.definitions.states.State;
import demo.chess.game.Game;
import demo.chess.game.impl.Simulation;

@Service
public class AnalysisReplayService {

    private final GameService gameService;
    private final EngineSettingsService engineSettingsService;
    private final EvaluationService evaluationService;
    private final UciGameService uciGameService;
    private AnalysisReplaySession session;

    /**
     * Creates a new AnalysisReplayService instance.
     * @param gameService the game service
     * @param engineSettingsService the engine settings service
     * @param evaluationService the evaluation service
     * @param uciGameService the uci game service
     */
    public AnalysisReplayService(
            GameService gameService,
            EngineSettingsService engineSettingsService,
            EvaluationService evaluationService,
            UciGameService uciGameService) {
        this.gameService = gameService;
        this.engineSettingsService = engineSettingsService;
        this.evaluationService = evaluationService;
        this.uciGameService = uciGameService;
    }

    /**
     * Performs the start operation.
     * @param settings the settings
     * @return the result of the operation
     */
    public synchronized AnalysisReplayStepDto start(AnalysisReplaySettingsDto settings)
            throws NoMoveFoundException, IOException {
        closeSessionEngine();
        evaluationService.stopLiveEvaluation();

        List<Move> moveListSnapshot = uciGameService.getAnalysisMoveListSnapshot();
        String requestedProfileId = settings != null ? settings.getEngineProfileId() : null;
        int depth = settings != null ? Math.max(0, settings.getDepth()) : 0;
        int moveTimeSeconds = settings != null ? Math.max(1, settings.getMoveTimeSeconds()) : 5;
        String engineProfileId = engineSettingsService.normalizeDeepAnalysisProfileId(requestedProfileId);
        UciEngineConfig engineConfig = engineSettingsService.getDeepAnalysisConfig(
                engineProfileId,
                depth,
                moveTimeSeconds);
        DeepAnalysisEngineSelection engineSelection = createDeepAnalysisEngine(engineConfig.getEngine());
        String engineName = engineConfig.getEngineName();

        AnalysisReplaySession newSession = new AnalysisReplaySession(
                moveListSnapshot,
                Simulation.createSimulation(),
                engineSelection.engine,
                engineConfig,
                engineName);

        newSession.profile.add(new AnalysisProfilePointDto(
                0,
                null,
                null,
                "Start",
                0.0,
                0.5,
                0));

        this.session = newSession;
        return toStepDto(newSession, false, null, null, null, 0.0, 0.5, 0, "Analysis replay started.");
    }

    /**
     * Performs the next operation.
     * @return the result of the operation
     */
    public synchronized AnalysisReplayStepDto next() throws NoMoveFoundException, IOException {
        if (session == null || !session.active) {
            return inactiveStep("No active analysis replay.");
        }

        if (session.currentPly >= session.originalMoves.size()) {
            session.active = false;
            closeSessionEngine();
            return toStepDto(session, true, null, null, null, latestEvaluation(session), latestBar(session), latestDepth(session),
                    "Analysis replay finished.");
        }

        Move originalMove = session.originalMoves.get(session.currentPly);
        Move replayMove = session.replayGame.getPlayer().getMoveInSimulation(session.replayGame, originalMove);

        String from = replayMove.getSource() != null ? replayMove.getSource().getName() : null;
        String to = replayMove.getTarget() != null ? replayMove.getTarget().getName() : null;
        String san = originalMove.toString();

        session.replayGame.apply(replayMove);
        session.currentPly++;

        AnalysisEvaluation evaluation = analyzeCurrentReplayPosition(session);

        session.profile.add(new AnalysisProfilePointDto(
                session.currentPly,
                from,
                to,
                san,
                Math.round(evaluation.evaluation * 100.0) / 100.0,
                evaluation.bar,
                evaluation.depth,
                evaluation.lines));

        boolean done = session.currentPly >= session.originalMoves.size();
        if (done) {
            session.active = false;
            closeSessionEngine();
        }

        return toStepDto(
                session,
                done,
                from,
                to,
                san,
                evaluation.evaluation,
                evaluation.bar,
                evaluation.depth,
                done ? "Analysis replay finished." : null);
    }

    /**
     * Performs the state operation.
     * @return the result of the operation
     */
    public synchronized AnalysisReplayStepDto state() {
        if (session == null) {
            return inactiveStep("No active analysis replay.");
        }

        return toStepDto(
                session,
                !session.active,
                null,
                null,
                null,
                latestEvaluation(session),
                latestBar(session),
                latestDepth(session),
                null);
    }

    /**
     * Returns whether this object can cel.
     * @return true when the condition is satisfied; otherwise false
     */
    public synchronized AnalysisReplayStepDto cancel() {
        if (session == null) {
            return inactiveStep("No active analysis replay.");
        }

        session.active = false;
        closeSessionEngine();
        return toStepDto(
                session,
                true,
                null,
                null,
                null,
                latestEvaluation(session),
                latestBar(session),
                latestDepth(session),
                "Analysis replay cancelled.");
    }

    /**
     * Creates the deep analysis engine.
     * @param enginePath the engine path
     * @return the result of the operation
     */
    private DeepAnalysisEngineSelection createDeepAnalysisEngine(String enginePath) {
        String effectivePath = enginePath == null || enginePath.isBlank()
                ? engineSettingsService.getDefaultEnginePath()
                : enginePath.trim();
        try {
            DeepAnalysisUciEngine engine = new DeepAnalysisUciEngine(effectivePath);
            engine.setManagementLabel("deep analysis");
            return new DeepAnalysisEngineSelection(engine, effectivePath);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not start deep analysis engine at " + effectivePath, ex);
        }
    }

    /**
     * Performs the analyze current replay position operation.
     * @param source the source
     * @return the result of the operation
     */
    private AnalysisEvaluation analyzeCurrentReplayPosition(AnalysisReplaySession source)  throws NoMoveFoundException{
        AnalysisEvaluation terminalEvaluation = evaluateTerminalPosition(source);
        if (terminalEvaluation != null) {
            return terminalEvaluation;
        }

        try {
            source.engine.clearChachedLines();
            List<EngineLine> bestLines = source.engine.getBestLines(
                    source.replayGame,
                    source.engineConfig);

            if (bestLines == null || bestLines.isEmpty()) {
                return new AnalysisEvaluation(0.0, 0.5, 0, List.of());
            }

            double eval = bestLines.get(0).getEvaluation();
            double bar = mapEvalToBar(eval);
            int depth = bestLines.get(0).getDepth();

            List<EngineLineDto> lines = new ArrayList<>();
            for (EngineLine line : bestLines) {
                double lineEval = line.getEvaluation();
                int lineDepth = line.getDepth();
                Integer mateDistance = line.getMateDistance();
                String movesUci = line.getMoves();
                EngineLineDisplayData displayData = convertEngineLine(
                        Simulation.forkDummyFrom(source.replayGame.getMoveList()),
                        movesUci);
                double roundedEval = Math.round(lineEval * 100.0) / 100.0;
                lines.add(new EngineLineDto(
                        roundedEval,
                        lineDepth,
                        mateDistance,
                        displayData.moves,
                        displayData.positions));
            }

            return new AnalysisEvaluation(eval, bar, depth, lines);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            safeStop(source.engine);
            return new AnalysisEvaluation(0.0, 0.5, 0, List.of());
        } catch (ExecutionException | IOException e) {
            safeStop(source.engine);
            return new AnalysisEvaluation(0.0, 0.5, 0, List.of());
        }
    }

    /**
     * Evaluates the terminal position.
     * @param source the source
     * @return the result of the operation
     */
    private AnalysisEvaluation evaluateTerminalPosition(AnalysisReplaySession source) {
        if (source == null || source.replayGame == null) {
            return null;
        }

        AnalysisEvaluation explicitStateEvaluation = evaluateExplicitTerminalState(source);
        if (explicitStateEvaluation != null) {
            return explicitStateEvaluation;
        }

        return evaluateTerminalSimulationPosition(source);
    }

    /**
     * Evaluates the explicit terminal state.
     * @param source the source
     * @return the result of the operation
     */
    private AnalysisEvaluation evaluateExplicitTerminalState(AnalysisReplaySession source) {
        State state = source.replayGame.getState();
        if (state == null) {
            return null;
        }

        if (state == State.BLACK_MATED || state == State.BLACK_RESIGNED) {
            return new AnalysisEvaluation(100.0, 1.0, latestDepth(source), List.of());
        }

        if (state == State.WHITE_MATED || state == State.WHITE_RESIGNED) {
            return new AnalysisEvaluation(-100.0, 0.0, latestDepth(source), List.of());
        }

        if (state == State.STALEMATE
                || state == State.DRAW_BY_50_MOVES_RULE
                || state == State.DRAW_BY_THREEFOLD_REPETITION) {
            return new AnalysisEvaluation(0.0, 0.5, latestDepth(source), List.of());
        }

        return null;
    }

    /**
     * Evaluates the terminal simulation position.
     * @param source the source
     * @return the result of the operation
     */
    private AnalysisEvaluation evaluateTerminalSimulationPosition(AnalysisReplaySession source) {
        Player playerToMove = source.replayGame.getPlayer();
        if (playerToMove == null || playerToMove.getKing() == null || playerToMove.getKing().getField() == null) {
            return null;
        }

        try {
            if (!playerToMove.getValidMoves(source.replayGame).isEmpty()) {
                return null;
            }
        } catch (NoMoveFoundException | IOException e) {
            return null;
        }

        Player opponent = playerToMove == source.replayGame.getWhitePlayer()
                ? source.replayGame.getBlackPlayer()
                : source.replayGame.getWhitePlayer();

        boolean kingIsAttacked = opponent.getSimpleMoves().stream()
                .map(Move::getTarget)
                .anyMatch(playerToMove.getKing().getField()::equals);

        if (!kingIsAttacked) {
            return new AnalysisEvaluation(0.0, 0.5, latestDepth(source), List.of());
        }

        if (playerToMove == source.replayGame.getWhitePlayer()) {
            return new AnalysisEvaluation(-100.0, 0.0, latestDepth(source), List.of());
        }

        return new AnalysisEvaluation(100.0, 1.0, latestDepth(source), List.of());
    }

    /**
     * Converts the engine line.
     * @param currentGame the current game
     * @param uciMoves the uci moves
     * @return the result of the operation
     */
    private EngineLineDisplayData convertEngineLine(Game currentGame, String uciMoves) {
        if (uciMoves == null || uciMoves.isBlank()) {
            return new EngineLineDisplayData("", currentGame != null ? List.of(toPositionString(currentGame)) : List.of());
        }

        if (currentGame == null) {
            return new EngineLineDisplayData(uciMoves, List.of());
        }

        try {
            StringBuilder result = new StringBuilder();
            List<String> positions = new ArrayList<>();
            positions.add(toPositionString(currentGame));

            String[] tokens = uciMoves.split("\\s+");
            for (String token : tokens) {
                if (token == null || token.isBlank()) {
                    continue;
                }

                Move move = findMoveByUci(currentGame, token);
                if (move == null) {
                    break;
                }

                String displayMove = toDisplaySan(currentGame, move);
                if (!displayMove.isBlank()) {
                    if (result.length() > 0) {
                        result.append(' ');
                    }
                    result.append(displayMove);
                }

                currentGame.apply(move);
                positions.add(toPositionString(currentGame));
            }

            return new EngineLineDisplayData(result.length() > 0 ? result.toString() : uciMoves, positions);
        } catch (Exception ignored) {
            return new EngineLineDisplayData(uciMoves, List.of(toPositionString(currentGame)));
        }
    }

    /**
     * Performs the to position string operation.
     * @param game the game
     * @return the result of the operation
     */
    private String toPositionString(Game game) {
        if (game == null || game.getChessBoard() == null) {
            return "";
        }

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
        }

        return piece.getColor() == Color.WHITE ? Character.toUpperCase(pieceChar) : pieceChar;
    }

    /**
     * Finds the move by uci.
     * @param game the game
     * @param uci the uci
     * @return the result of the operation
     */
    private Move findMoveByUci(Game game, String uci) {
        if (game == null || uci == null || uci.isBlank()) {
            return null;
        }

        try {
            for (Move candidate : game.getPlayer().getValidMoves(game)) {
                if (uci.equals(candidate.toString())) {
                    return candidate;
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Performs the to display san operation.
     * @param game the game
     * @param move the move
     * @return the result of the operation
     */
    private String toDisplaySan(Game game, Move move) {
        if (move == null || move.getSource() == null || move.getTarget() == null || move.getPiece() == null) {
            return "";
        }

        Field source = move.getSource();
        Field target = move.getTarget();
        Piece piece = move.getPiece();

        if (move instanceof Castling) {
            return target.getFile() > source.getFile() ? "0-0" : "0-0-0";
        }

        String piecePrefix = getPiecePrefix(piece);
        String sourceDisambiguation = getSourceDisambiguation(game, move);
        boolean capture = target.getPiece() != null || move instanceof EnPassant;
        String captureMarker = capture ? "x" : "";
        String targetName = target.toString();
        String postFix = "";

        if (piece.getType() == PieceType.PAWN && capture) {
            sourceDisambiguation = source.toString().substring(0, 1);
        }

        if (move instanceof EnPassant) {
            postFix = " e.p.";
        }

        if (move instanceof Promotion) {
            Promotion promotion = (Promotion) move;
            if (promotion.getPromotedPiece() != null) {
                postFix = "=" + getPiecePrefix(promotion.getPromotedPiece());
            }
        }

        return piecePrefix + sourceDisambiguation + captureMarker + targetName + postFix;
    }

    /**
     * Returns the source disambiguation.
     * @param game the game
     * @param move the move
     * @return the source disambiguation
     */
    private String getSourceDisambiguation(Game game, Move move) {
        Piece piece = move.getPiece();
        if (piece == null || piece.getType() == PieceType.PAWN || move.getTarget() == null) {
            return "";
        }

        List<Move> competingMoves = new ArrayList<>();
        try {
            for (Move candidate : game.getPlayer().getValidMoves(game)) {
                if (candidate.getPiece() == null
                        || candidate.getSource() == null
                        || candidate.getTarget() == null) {
                    continue;
                }

                if (candidate.getSource().equals(move.getSource())
                        && candidate.getTarget().equals(move.getTarget())) {
                    continue;
                }

                if (candidate.getTarget().equals(move.getTarget())
                        && candidate.getPiece().getType() == piece.getType()) {
                    competingMoves.add(candidate);
                }
            }
        } catch (Exception ignored) {
            return "";
        }

        if (competingMoves.isEmpty()) {
            return "";
        }

        boolean sameFileExists = competingMoves.stream()
                .anyMatch(candidate -> candidate.getSource().getFile() == move.getSource().getFile());
        boolean sameRankExists = competingMoves.stream()
                .anyMatch(candidate -> candidate.getSource().getRank() == move.getSource().getRank());

        if (sameFileExists && sameRankExists) {
            return move.getSource().toString();
        }

        if (sameFileExists) {
            return move.getSource().toString().substring(1, 2);
        }

        return move.getSource().toString().substring(0, 1);
    }

    /**
     * Returns the piece prefix.
     * @param piece the piece
     * @return the piece prefix
     */
    private String getPiecePrefix(Piece piece) {
        if (piece == null || piece.getType() == null || piece.getType() == PieceType.PAWN) {
            return "";
        }

        return getUnicodeSymbol(piece.getType(), piece.getColor());
    }

    /**
     * Returns the unicode symbol.
     * @param pieceType the piece type
     * @param color the color
     * @return the unicode symbol
     */
    private String getUnicodeSymbol(PieceType pieceType, Color color) {
        if (pieceType == null || color == null) {
            return "";
        }

        switch (color) {
            case WHITE:
                switch (pieceType) {
                    case KING:
                        return "♔";
                    case QUEEN:
                        return "♕";
                    case ROOK:
                        return "♖";
                    case BISHOP:
                        return "♗";
                    case KNIGHT:
                        return "♘";
                    default:
                        return "";
                }
            case BLACK:
                switch (pieceType) {
                    case KING:
                        return "♚";
                    case QUEEN:
                        return "♛";
                    case ROOK:
                        return "♜";
                    case BISHOP:
                        return "♝";
                    case KNIGHT:
                        return "♞";
                    default:
                        return "";
                }
            default:
                return "";
        }
    }

    /**
     * Maps the eval to bar.
     * @param eval the eval
     * @return the result of the operation
     */
    private double mapEvalToBar(double eval) {
        if (eval >= 99d) {
            return 1.0;
        }
        if (eval <= -99d) {
            return 0.0;
        }
        return 0.5 + Math.atan(Math.tan(Math.PI / 10.0) * eval) / Math.PI;
    }

    /**
     * Performs the latest evaluation operation.
     * @param source the source
     * @return the result of the operation
     */
    private double latestEvaluation(AnalysisReplaySession source) {
        if (source == null || source.profile.isEmpty()) {
            return 0.0;
        }
        return source.profile.get(source.profile.size() - 1).getEvaluation();
    }

    /**
     * Performs the latest bar operation.
     * @param source the source
     * @return the result of the operation
     */
    private double latestBar(AnalysisReplaySession source) {
        if (source == null || source.profile.isEmpty()) {
            return 0.5;
        }
        return source.profile.get(source.profile.size() - 1).getBar();
    }

    /**
     * Performs the latest depth operation.
     * @param source the source
     * @return the result of the operation
     */
    private int latestDepth(AnalysisReplaySession source) {
        if (source == null || source.profile.isEmpty()) {
            return 0;
        }
        return source.profile.get(source.profile.size() - 1).getDepth();
    }

    /**
     * Performs the inactive step operation.
     * @param message the message
     * @return the result of the operation
     */
    private AnalysisReplayStepDto inactiveStep(String message) {
        return new AnalysisReplayStepDto(
                false,
                true,
                0,
                0,
                null,
                null,
                null,
                0.0,
                0.5,
                0,
                null,
                null,
                List.of(),
                message);
    }

    /**
     * Performs the to step dto operation.
     * @param source the source
     * @param done the done
     * @param from the from
     * @param to the to
     * @param san the san
     * @param evaluation the evaluation
     * @param bar the bar
     * @param depth the depth
     * @param message the message
     * @return the result of the operation
     */
    private AnalysisReplayStepDto toStepDto(
            AnalysisReplaySession source,
            boolean done,
            String from,
            String to,
            String san,
            double evaluation,
            double bar,
            int depth,
            String message) {
        BoardDto board = gameService.getBoardView(source.replayGame);
        return new AnalysisReplayStepDto(
                source.active,
                done,
                source.originalMoves.size(),
                source.currentPly,
                from,
                to,
                san,
                Math.round(evaluation * 100.0) / 100.0,
                bar,
                depth,
                source.engineName,
                board,
                new ArrayList<>(source.profile),
                message);
    }

    /**
     * Closes the session engine.
     */
    private void closeSessionEngine() {
        if (session == null || session.engine == null) {
            return;
        }
        // DeepAnalysisUciEngine.stopEvaluation() is terminal and closes the
        // underlying UCI process. Avoid a second close/quit here.
        safeStop(session.engine);
    }

    /**
     * Performs the safe stop operation.
     * @param engine the engine
     */
    private void safeStop(DeepAnalysisEngine engine) {
        if (engine == null) {
            return;
        }
        try {
            engine.stopEvaluation();
        } catch (Exception ignored) {
        }
    }

    private static class DeepAnalysisEngineSelection {
        private final DeepAnalysisEngine engine;
        private final String enginePath;

        /**
         * Creates a new DeepAnalysisEngineSelection instance.
         * @param engine the engine
         * @param enginePath the engine path
         */
        private DeepAnalysisEngineSelection(DeepAnalysisEngine engine, String enginePath) {
            this.engine = engine;
            this.enginePath = enginePath;
        }
    }

    private static class EngineLineDisplayData {
        private final String moves;
        private final List<String> positions;

        /**
         * Creates a new EngineLineDisplayData instance.
         * @param moves the moves
         * @param positions the positions
         */
        private EngineLineDisplayData(String moves, List<String> positions) {
            this.moves = moves;
            this.positions = positions != null ? positions : List.of();
        }
    }

    private static class AnalysisEvaluation {
        private final double evaluation;
        private final double bar;
        private final int depth;
        private final List<EngineLineDto> lines;

        /**
         * Creates a new AnalysisEvaluation instance.
         * @param evaluation the evaluation
         * @param bar the bar
         * @param depth the depth
         * @param lines the lines
         */
        private AnalysisEvaluation(double evaluation, double bar, int depth, List<EngineLineDto> lines) {
            this.evaluation = evaluation;
            this.bar = bar;
            this.depth = depth;
            this.lines = lines;
        }
    }

    private static class AnalysisReplaySession {
        private final List<Move> originalMoves;
        private final Game replayGame;
        private final DeepAnalysisEngine engine;
        private final UciEngineConfig engineConfig;
        private final String engineName;
        private final List<AnalysisProfilePointDto> profile = new ArrayList<>();
        private int currentPly = 0;
        private boolean active = true;

        /**
         * Creates a new AnalysisReplaySession instance.
         * @param originalMoves the original moves
         * @param replayGame the replay game
         * @param engine the engine
         * @param engineConfig the engine config
         * @param engineName the engine name
         */
        private AnalysisReplaySession(
                List<Move> originalMoves,
                Game replayGame,
                DeepAnalysisEngine engine,
                UciEngineConfig engineConfig,
                String engineName) {
            this.originalMoves = originalMoves;
            this.replayGame = replayGame;
            this.engine = engine;
            this.engineConfig = engineConfig;
            this.engineName = engineName;
        }
    }
}
