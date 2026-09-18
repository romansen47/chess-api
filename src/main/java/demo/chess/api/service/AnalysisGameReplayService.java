package demo.chess.api.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;

import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.definitions.moves.Move;
import demo.chess.game.Game;
import demo.chess.game.LegalMoveResolver;
import demo.chess.game.impl.Simulation;

/**
 * Canonical reconstruction boundary for the game selected for analysis.
 *
 * <p>All historical analysis positions start from the
 * {@link AnalysisGameContext#startingPosition()} carried by
 * {@link UciGameService}. No caller is allowed to recreate analysis history
 * from an implicit classical starting position. This is especially important
 * for Chess960, where a perfectly legal move history can be nonsensical when
 * replayed from Scharnagl position 518.</p>
 */
@Service
public class AnalysisGameReplayService {

    private final UciGameService uciGameService;

    public AnalysisGameReplayService(UciGameService uciGameService) {
        this.uciGameService = uciGameService;
    }

    /** Returns one atomic snapshot of the currently selected analysis game. */
    AnalysisGameContext currentContext() {
        AnalysisGameContext context = uciGameService.getAnalysisGameContext();
        if (context != null) {
            return context;
        }

        // Compatibility for direct tests/embedders that mock the historical
        // public getters instead of the package-private atomic snapshot method.
        // Production UciGameService always returns the atomic context above.
        return new AnalysisGameContext(
                uciGameService.getAnalysisStartingPosition(),
                uciGameService.getAnalysisMoveListSnapshot());
    }

    /**
     * Reconstructs the selected analysis game at the requested ply.
     *
     * @param ply number of original moves to apply
     * @return reconstructed simulation
     */
    public Simulation createPositionAtPly(int ply)
            throws NoMoveFoundException, IOException {
        return createPositionAtPly(currentContext(), ply);
    }

    /**
     * Reconstructs one supplied immutable analysis snapshot at the requested
     * ply. Long-running workflows can therefore keep one consistent context.
     */
    Simulation createPositionAtPly(AnalysisGameContext context, int ply)
            throws NoMoveFoundException, IOException {
        AnalysisGameContext source = context != null
                ? context
                : new AnalysisGameContext(null, List.of());
        validatePly(ply, source.moves().size());

        Simulation simulation = Simulation.createSimulation(source.startingPosition());
        for (int index = 0; index < ply; index++) {
            simulation.apply(mapOriginalMove(simulation, source.moves().get(index)));
        }
        return simulation;
    }

    /**
     * Reconstructs a temporary variation after an original-game anchor.
     *
     * @param anchorPly original-game ply at which the variation begins
     * @param variationMoves canonical UCI moves after the anchor
     * @return reconstructed variation simulation
     */
    public Simulation createVariationAtPly(int anchorPly, List<String> variationMoves)
            throws NoMoveFoundException, IOException {
        AnalysisGameContext context = currentContext();
        Simulation simulation = createPositionAtPly(context, anchorPly);
        if (variationMoves == null) return simulation;

        for (String uci : variationMoves) {
            if (uci == null || uci.isBlank()) continue;
            Move variationMove = LegalMoveResolver.resolveUci(simulation, uci);
            simulation.apply(variationMove);
        }
        return simulation;
    }

    /** Maps one original move onto a replay game with the same start context. */
    Move mapOriginalMove(Game replayGame, Move originalMove)
            throws NoMoveFoundException, IOException {
        if (replayGame == null || originalMove == null) {
            throw new NoMoveFoundException("Analysis replay move must not be null");
        }
        Move replayMove = replayGame.getPlayer().getMoveInSimulation(replayGame, originalMove);
        if (replayMove == null) {
            throw new NoMoveFoundException(
                    "Could not map analysis replay move: " + originalMove);
        }
        return replayMove;
    }

    private void validatePly(int ply, int moveCount) {
        if (ply < 0 || ply > moveCount) {
            throw new IllegalArgumentException(
                    "Analysis ply must be between 0 and " + moveCount + ", got " + ply);
        }
    }
}
