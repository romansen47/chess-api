package demo.chess.api.service;

import demo.chess.definitions.states.State;

/**
 * Maps terminal chess states to the analysis evaluation convention.
 */
final class TerminalEvaluationMapper {

    private TerminalEvaluationMapper() {
    }

    /**
     * Returns the terminal evaluation score used by analysis views.
     *
     * @param state terminal chess state
     * @return +100 for a White win, -100 for a Black win, 0 for a draw,
     *         or {@code null} when the state is not position-evaluable
     */
    static Double toEvaluation(State state) {
        if (state == null) {
            return null;
        }

        return switch (state) {
            case BLACK_MATED, BLACK_RESIGNED -> 100.0;
            case WHITE_MATED, WHITE_RESIGNED -> -100.0;
            case STALEMATE, DRAW_BY_50_MOVES_RULE, DRAW_BY_THREEFOLD_REPETITION -> 0.0;
            case LOST_ON_TIME -> null;
        };
    }
}
