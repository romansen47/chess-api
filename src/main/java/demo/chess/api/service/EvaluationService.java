package demo.chess.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import demo.chess.api.dto.EngineEvaluationDto;
import demo.chess.api.dto.EngineLineDto;
import demo.chess.definitions.Color;
import demo.chess.definitions.PieceType;
import demo.chess.definitions.engines.EngineConfig;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.EvaluationEngine;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.impl.EvaluationUciEngine;
import demo.chess.definitions.fields.Field;
import demo.chess.definitions.moves.Castling;
import demo.chess.definitions.moves.EnPassant;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.moves.Promotion;
import demo.chess.definitions.pieces.Piece;
import demo.chess.game.Game;
import demo.chess.game.impl.Simulation;

@Service
public class EvaluationService {

    private static final Log logger = LogFactory.getLog(EvaluationService.class);

    private final GameService gameService;
    private EvaluationEngine evaluationEngine;
    private final EngineRuntimeSelectionService engineRuntimeSelectionService;
    private final LiveEvaluationStreamService liveEvaluationStreamService;
    private final ExecutorService liveEvaluationPushExecutor;
    private String currentEvaluationEnginePath;
    private long lastSeenSettingsVersion = -1L;

    /**
     * Creates a new EvaluationService instance.
     * @param gameService the game service
     * @param engineRuntimeSelectionService runtime engine profile selections
     * @param liveEvaluationStreamService SSE stream publisher
     */
    public EvaluationService(
            GameService gameService,
            EngineRuntimeSelectionService engineRuntimeSelectionService,
            LiveEvaluationStreamService liveEvaluationStreamService) {
        this.gameService = gameService;
        this.engineRuntimeSelectionService = engineRuntimeSelectionService;
        this.liveEvaluationStreamService = liveEvaluationStreamService;
        this.currentEvaluationEnginePath = engineRuntimeSelectionService.getEvaluationEnginePath();
        this.evaluationEngine = null;
        this.liveEvaluationPushExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "live-evaluation-sse-publisher");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Returns the evaluation.
     * @return the evaluation
     */
    public synchronized EngineEvaluationDto getEvaluation() {
        Game game = gameService.getCurrentGame();
        EngineConfig engineConfig = engineRuntimeSelectionService.getEvaluationConfig();
        EvaluationEngine engine = getEvaluationEngine();
        long settingsVersion = engineRuntimeSelectionService.getEvaluationVersion();

        logger.debug("Requesting best lines from engine (single snapshot)...");

        if (settingsVersion != lastSeenSettingsVersion) {
            engine.clearChachedLines();
            lastSeenSettingsVersion = settingsVersion;
        }

        List<EngineLine> bestLines;

        try {
            bestLines = engine.getBestLines(game, engineConfig);
        } catch (Exception e) {
            logger.error("Engine error while getting best lines: " + e.getMessage());
            e.printStackTrace();
            return new EngineEvaluationDto(0.0, 0.5, List.of());
        }

        int size = (bestLines == null) ? -1 : bestLines.size();
        logger.debug("Engine returned lines size=" + size);

        if (bestLines == null || bestLines.isEmpty()) {
            return new EngineEvaluationDto(0.0, 0.5, List.of());
        }

        return toEvaluationDto(
                game,
                bestLines,
                engineRuntimeSelectionService.getEvaluationEngineName());
    }

    /**
     * Evaluates the game for analysis.
     * @param game the game
     * @param engineConfig the engine config
     * @param moveTimeMillis the move time millis
     * @return the result of the operation
     */
    public synchronized EngineEvaluationDto evaluateGameForAnalysis(
            Game game,
            UciEngineConfig engineConfig,
            int moveTimeMillis) {
        EvaluationEngine engine = getEvaluationEngine();
        int safeMoveTimeMillis = Math.max(100, moveTimeMillis);

        try {
            engine.clearChachedLines();
            engine.getBestLines(game, engineConfig);
            Thread.sleep(safeMoveTimeMillis);
            List<EngineLine> bestLines = engine.getBestLines(game, engineConfig);
            engine.stopEvaluation();

            if (bestLines == null || bestLines.isEmpty()) {
                return new EngineEvaluationDto(0.0, 0.5, List.of());
            }

            double eval = bestLines.get(0).getEvaluation();
            double bar = mapEvalToBar(eval);

            List<EngineLineDto> lines = new ArrayList<>();
            for (EngineLine line : bestLines) {
                double lineEval = line.getEvaluation();
                int depth = line.getDepth();
                Integer mateDistance = line.getMateDistance();
                String movesUci = line.getMoves();
                double roundedEval = Math.round(lineEval * 100.0) / 100.0;
                lines.add(new EngineLineDto(roundedEval, depth, mateDistance, movesUci));
            }

            return new EngineEvaluationDto(eval, bar, lines);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            try {
                engine.stopEvaluation();
            } catch (Exception ignored) {
            }
            return new EngineEvaluationDto(0.0, 0.5, List.of());
        } catch (Exception e) {
            logger.error("Engine error while replay-analyzing position: " + e.getMessage());
            try {
                engine.stopEvaluation();
            } catch (Exception ignored) {
            }
            return new EngineEvaluationDto(0.0, 0.5, List.of());
        }
    }

    /**
     * Resets the for new game.
     */
    public synchronized void resetForNewGame() {
        logger.info("Resetting evaluation engine for new game");

        closeEvaluationEngine(evaluationEngine);
        evaluationEngine = null;
        currentEvaluationEnginePath = engineRuntimeSelectionService.getEvaluationEnginePath();
        lastSeenSettingsVersion = -1L;
    }

    /**
     * Stops the live evaluation.
     */
    public synchronized void stopLiveEvaluation() {
        logger.info("Stopping live evaluation engine");
        closeEvaluationEngine(evaluationEngine);
        evaluationEngine = null;
        lastSeenSettingsVersion = -1L;
    }

    /**
     * Receives one completed engine depth. The UCI reader thread only copies the
     * tiny scalar values required by the bar and queues the actual SSE send on a
     * separate worker.
     * @param positionKey move-list key of the evaluated position
     * @param lines immutable engine-line snapshot
     */
    private void handleEvaluationUpdate(String positionKey, List<EngineLine> lines) {
        if (lines == null || lines.isEmpty() || !liveEvaluationStreamService.hasSubscribers()) {
            return;
        }

        EngineLine principalLine = lines.get(0);
        int depth = principalLine.getDepth();
        if (!shouldPushDepth(depth)) {
            return;
        }

        double evaluation = principalLine.getEvaluation();
        liveEvaluationPushExecutor.execute(
                () -> publishBarSnapshot(positionKey, evaluation, depth));
    }

    /**
     * Push schedule requested for the SSE experiment: 5, 10, 15 and every
     * completed depth after 15.
     */
    private boolean shouldPushDepth(int depth) {
        return depth == 5 || depth == 10 || depth >= 15;
    }

    /**
     * Publishes one bar-only snapshot if the game has not moved on meanwhile.
     * @param positionKey move-list key of the evaluated position
     * @param evaluation evaluation in pawns
     * @param depth search depth
     */
    private void publishBarSnapshot(String positionKey, double evaluation, int depth) {
        try {
            Game currentGame = gameService.getCurrentGame();
            if (!positionKey.equals(currentGame.getMoveList().toString())) {
                return;
            }

            liveEvaluationStreamService.publish(
                    evaluation,
                    mapEvalToBar(evaluation),
                    depth);
        } catch (Exception e) {
            logger.debug("Could not publish live evaluation bar SSE snapshot: " + e.getMessage());
        }
    }

    /**
     * Converts one normal polled engine-line snapshot into the frontend DTO.
     * This path intentionally remains independent of the SSE bar updates.
     */
    private EngineEvaluationDto toEvaluationDto(
            Game game,
            List<EngineLine> bestLines,
            String engineName) {
        double eval = bestLines.get(0).getEvaluation();
        double bar = mapEvalToBar(eval);

        List<EngineLineDto> lines = new ArrayList<>();
        for (EngineLine line : bestLines) {
            double lineEval = line.getEvaluation();
            int depth = line.getDepth();
            Integer mateDistance = line.getMateDistance();
            String movesUci = line.getMoves();
            String movesSan;

            try {
                movesSan = convertLineToSan(Simulation.forkDummyFrom(game.getMoveList()), movesUci);
            } catch (Exception ex) {
                logger.error("convertLineToSan failed, fallback to UCI: " + ex.getMessage());
                movesSan = movesUci;
            }

            double roundedEval = Math.round(lineEval * 100.0) / 100.0;
            lines.add(new EngineLineDto(roundedEval, depth, mateDistance, movesSan));
        }

        EngineEvaluationDto result = new EngineEvaluationDto(eval, bar, lines);
        result.setEngineName(engineName);
        return result;
    }

    /**
     * Returns the evaluation engine.
     * @return the evaluation engine
     */
    private synchronized EvaluationEngine getEvaluationEngine() {
        String configuredPath = engineRuntimeSelectionService.getEvaluationEnginePath();
        if (evaluationEngine == null || !configuredPath.equals(currentEvaluationEnginePath)) {
            closeEvaluationEngine(evaluationEngine);
            currentEvaluationEnginePath = configuredPath;
            evaluationEngine = createEvaluationEngine(configuredPath);
            lastSeenSettingsVersion = -1L;
        }
        return evaluationEngine;
    }

    /**
     * Creates the evaluation engine.
     * @param enginePath the engine path
     * @return the result of the operation
     */
    private EvaluationEngine createEvaluationEngine(String enginePath) {
        logger.info("Initializing evaluation engine at path: " + enginePath);
        try {
            EvaluationUciEngine engine = new EvaluationUciEngine(enginePath);
            engine.setManagementLabel("evaluation");
            engine.setEvaluationUpdateListener(this::handleEvaluationUpdate);
            return engine;
        } catch (Exception e) {
            logger.error("Could not start evaluation engine: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Could not start evaluation engine at " + enginePath, e);
        }
    }

    /**
     * Closes the evaluation engine.
     * @param engine the engine
     */
    private void closeEvaluationEngine(EvaluationEngine engine) {
        if (engine == null) {
            return;
        }
        try {
            engine.stopEvaluation();
        } catch (Exception e) {
            logger.warn("Could not stop old evaluation engine: " + e.getMessage());
        }
        try {
            engine.close();
        } catch (Exception e) {
            logger.warn("Could not close old evaluation engine: " + e.getMessage());
        }
    }

    /**
     * Converts the line to san.
     * @param currentGame the current game
     * @param uciMoves the uci moves
     * @return the result of the operation
     */
    private String convertLineToSan(Game currentGame, String uciMoves) throws Exception {
        if (uciMoves == null || uciMoves.isBlank()) {
            return "";
        }

        try {
            Game tmpGame = currentGame;

            // Die aktuelle Partie wurde dem Dummy-Game bereits vor dem Aufruf nachgespielt.
            // Hier wird nur noch die Engine-Linie darauf angewendet.
            //
            // Wichtig: Für die Anzeige der Engine-Lines verwenden wir bewusst nicht
            // tmpGame.getSanMoveList(). Die SAN-Erzeugung im Core-Projekt chess bleibt
            // unverändert; die korrekte Disambiguierung für die UI erzeugen wir hier
            // in der API-Schicht. So bleiben alle Änderungen außerhalb von chess.
            StringBuilder sb = new StringBuilder();
            String[] tokens = uciMoves.split("\\s+");
            for (String token : tokens) {
                if (token == null || token.isBlank()) {
                    continue;
                }

                Move toApply = findMoveByUci(tmpGame, token);
                if (toApply == null) {
                    break;
                }

                String san = toDisplaySan(tmpGame, toApply);
                if (!san.isBlank()) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(san);
                }

                tmpGame.apply(toApply);
            }

            return sb.length() > 0 ? sb.toString() : uciMoves;
        } catch (Exception ex) {
            logger.error("convertLineToSan inner failure: " + ex.getMessage());
            return uciMoves;
        }
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
        } catch (Exception e) {
            logger.error("getSourceDisambiguation: " + e.getMessage());
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
     * @return the result of the operation
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
     * Finds the move by uci.
     * @param game the game
     * @param uci the uci
     * @return the result of the operation
     */
    private Move findMoveByUci(Game game, String uci) {
        if (uci == null || uci.isBlank()) {
            return null;
        }
        try {
            for (Move candidate : game.getPlayer().getValidMoves(game)) {
                if (uci.equals(candidate.toString())) {
                    return candidate;
                }
            }
        } catch (Exception e) {
            logger.error("findMoveByUci: " + e.getMessage());
        }
        return null;
    }

    /**
     * Maps the eval to bar.
     * @param eval the eval
     * @return the result of the operation
     */
    private double mapEvalToBar(double eval) {
        double ans = 0.5 + Math.atan(Math.tan(Math.PI / 10d) * eval) / Math.PI;
        if (ans < 0.0) {
            ans = 0.0;
        } else if (ans > 1.0) {
            ans = 1.0;
        }
        return ans;
    }
}
