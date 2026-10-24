package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import demo.chess.api.dto.AnalysisVariationMoveResultDto;
import demo.chess.api.dto.AnalysisVariationRequestDto;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.definitions.states.State;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.TerminalPositionEvaluator;
import demo.chess.game.impl.Simulation;
import demo.chess.notation.UciMoveCodec;

/**
 * Reconstructs temporary analysis variations from an original anchor ply and
 * an ordered list of UCI moves. No variation state is kept on the server.
 *
 * <p>Historical reconstruction is delegated to
 * {@link AnalysisGameReplayService}; this service never assumes classical
 * position 518 on its own.</p>
 */
@Service
public class AnalysisVariationService {

    private final AnalysisGameReplayService analysisGameReplayService;

    @Autowired
    public AnalysisVariationService(AnalysisGameReplayService analysisGameReplayService) {
        this.analysisGameReplayService = analysisGameReplayService;
    }

    /** Compatibility constructor retained for direct unit tests and embedders. */
    public AnalysisVariationService(UciGameService uciGameService) {
        this(new AnalysisGameReplayService(uciGameService));
    }

    public Simulation createVariationGame(int anchorPly, List<String> variationMoves)
            throws NoMoveFoundException, IOException {
        return analysisGameReplayService.createVariationAtPly(anchorPly, variationMoves);
    }

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
        String uci = UciMoveCodec.encode(simulation, selected);
        simulation.apply(selected);

        String sideToMove = simulation.getPlayer() != null && simulation.getPlayer().getColor() != null
                ? simulation.getPlayer().getColor().name().toLowerCase(Locale.ROOT)
                : null;
        State terminalState = TerminalPositionEvaluator.determineState(simulation);
        String gameState = terminalState != null ? terminalState.name() : null;

        return new AnalysisVariationMoveResultDto(
                true, null, request.getFrom(), request.getTo(), uci,
                sideToMove, toPositionString(simulation), gameState);
    }

    public String toPositionString(Simulation simulation) {
        return BoardPositionSerializer.toPositionString(simulation);
    }
}
