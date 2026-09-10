package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EvaluationBarMapperTest {

    private static final double EPSILON = 1.0e-12;

    @Test
    void mapsNeutralAndSmallEvaluationsSymmetrically() {
        assertEquals(0.5, EvaluationBarMapper.toBar(0.0), EPSILON);
        assertEquals(0.6, EvaluationBarMapper.toBar(1.0), EPSILON);
        assertEquals(0.4, EvaluationBarMapper.toBar(-1.0), EPSILON);
    }

    @Test
    void keepsNonMateScoresOnExistingArctangentCurve() {
        assertEquals(0.9900067834885857, EvaluationBarMapper.toBar(98.0), EPSILON);
        assertEquals(0.009993216511414327, EvaluationBarMapper.toBar(-98.0), EPSILON);
    }

    @Test
    void mapsMateThresholdAndBeyondToBarEndpoints() {
        assertEquals(1.0, EvaluationBarMapper.toBar(99.0), EPSILON);
        assertEquals(0.0, EvaluationBarMapper.toBar(-99.0), EPSILON);
        assertEquals(1.0, EvaluationBarMapper.toBar(100.0), EPSILON);
        assertEquals(0.0, EvaluationBarMapper.toBar(-100.0), EPSILON);
    }
}
