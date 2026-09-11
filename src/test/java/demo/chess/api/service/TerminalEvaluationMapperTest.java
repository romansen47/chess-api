package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import demo.chess.definitions.states.State;

class TerminalEvaluationMapperTest {

    @Test
    void mapsTerminalStatesToAnalysisScores() {
        assertEquals(Double.valueOf(100.0), TerminalEvaluationMapper.toEvaluation(State.BLACK_MATED));
        assertEquals(Double.valueOf(100.0), TerminalEvaluationMapper.toEvaluation(State.BLACK_RESIGNED));
        assertEquals(Double.valueOf(-100.0), TerminalEvaluationMapper.toEvaluation(State.WHITE_MATED));
        assertEquals(Double.valueOf(-100.0), TerminalEvaluationMapper.toEvaluation(State.WHITE_RESIGNED));
        assertEquals(Double.valueOf(0.0), TerminalEvaluationMapper.toEvaluation(State.STALEMATE));
        assertEquals(Double.valueOf(0.0), TerminalEvaluationMapper.toEvaluation(State.DRAW_BY_50_MOVES_RULE));
        assertEquals(Double.valueOf(0.0), TerminalEvaluationMapper.toEvaluation(State.DRAW_BY_THREEFOLD_REPETITION));
        assertEquals(Double.valueOf(0.0), TerminalEvaluationMapper.toEvaluation(State.DRAW_BY_INSUFFICIENT_MATERIAL));
    }

    @Test
    void ignoresNonPositionTimeLossAndMissingState() {
        assertNull(TerminalEvaluationMapper.toEvaluation(State.LOST_ON_TIME));
        assertNull(TerminalEvaluationMapper.toEvaluation(null));
    }
}
