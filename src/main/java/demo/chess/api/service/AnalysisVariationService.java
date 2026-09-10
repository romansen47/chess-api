package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.AnalysisVariationMoveResultDto;
import demo.chess.api.dto.AnalysisVariationRequestDto;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.states.State;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.TerminalPositionEvaluator;
import demo.chess.game.impl.Simulation;

/**
 * Reconstructs temporary analysis variations from an original anchor ply and
 * an ordered list of UCI moves. No variation state is kept on the server.
 */
@Service
public class AnalysisVariationService {

    private final UciGameService uciGameService;

    /**
     * Creates a new AnalysisVariationService instance.
     * @param uciGameService the source of the original analysis game
     */
    public AnalysisVariationService(UciGameService uciGameService) {
        this.uciGameService = uciGameService;
    }

    /**
     * Reconstructs the complete variation position.
     * @param anchorPly original-game ply from which the variation starts
     * @param variationMoves UCI moves after the anchor
     * @return reconstructed simulation
     */
    public Simulation createVariationGame(int anchorPly, List<String> variationMoves)
            throws NoMoveFoundException, IOException {
        List<Move> originalMoves = uciGameService.getAnalysisMoveListSnapshot();
        if (anchorPly < 0 || anchorPly > originalMoves.size()) {
            throw new IllegalArgumentException(
                    "Analysis anchor ply must be between 0 and " + originalMoves.size()
                            + ", got " + anchorPly);
        }

        Simulation simulation = Simulation.createSimulation();
        for (int index = 0; index < anchorPly; index++) {
            Move originalMove = originalMoves.get(index);
            Move replayMove = simulation.getPlayer().getMoveInSimulation(simulation, originalMove);
            simulation.apply(replayMove);
        }

        for (String uci : safeMoves(variationMoves)) {
            Move variationMove = LegalMoveResolver.resolveUci(simulation, uci);
            simulation.apply(variationMove);
        }

        return simulation;
    }

    /**
     * Returns legal target squares for a source square in the reconstructed
     * variation position.
     * @param request variation request
     * @return legal target squares
     */
    public List<String> getPossibleTargets(AnalysisVariationRequestDto request)
            throws NoMoveFoundException, IOException {
        if (request == null || request.getFrom() == null || request.getFrom().isBlank()) {
            return List.of();
        }

        Simulation simulation = createVariationGame(request.getAnchorPly(), request.getMoves());
        String from = request.getFrom().trim().toLowerCase(Locale.ROOT);
        List<String> targets = new ArrayList<>();

        for (Move move : simulation.getPlayer().getValidMoves(simulation)) {
            if (move.getSource() != null
                    && move.getTarget() != null
                    && from.equalsIgnoreCase(move.getSource().getName())) {
                targets.add(move.getTarget().getName());
            }
        }

        return targets;
    }

    /**
     * Applies one legal move to a reconstructed variation.
     * @param request variation plus requested move
     * @return resulting variation position
     */
    public AnalysisVariationMoveResultDto applyMove(AnalysisVariationRequestDto request)
            throws NoMoveFoundException, IOException {
        if (request == null || request.getFrom() == null || request.getTo() == null) {
            throw new NoMoveFoundException("from/to must not be null");
        }

        Simulation simulation = createVariationGame(request.getAnchorPly(), request.getMoves());
        Move selected = LegalMoveResolver.resolveCoordinates(
                simulation,
                request.getFrom(),
                request.getTo(),
                request.getPromotion());
        String uci = selected.toString();
        simulation.apply(selected);

        String sideToMove = simulation.getPlayer() != null && simulation.getPlayer().getColor() != null
                ? simulation.getPlayer().getColor().name().toLowerCase(Locale.ROOT)
                : null;
        State terminalState = TerminalPositionEvaluator.determineState(simulation);
        String gameState = terminalState != null ? terminalState.name() : null;

        return new AnalysisVariationMoveResultDto(
                true,
                null,
                request.getFrom(),
                request.getTo(),
                uci,
                sideToMove,
                toPositionString(simulation),
                gameState);
    }


    private List<String> safeMoves(List<String> moves) {
        return moves != null ? moves : List.of();
    }

    /**
     * Converts a reconstructed game into the frontend's compact 64-character
     * board representation.
     * @param simulation source game
     * @return board representation
     */
    public String toPositionString(Simulation simulation) {
        return BoardPositionSerializer.toPositionString(simulation);
    }
}
