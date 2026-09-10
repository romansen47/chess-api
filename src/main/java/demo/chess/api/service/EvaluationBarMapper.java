package demo.chess.api.service;

/**
 * Maps engine evaluations to the normalized frontend evaluation bar.
 */
final class EvaluationBarMapper {

    static final double MATE_EVALUATION_THRESHOLD = 99.0;

    private EvaluationBarMapper() {
    }

    static double toBar(double evaluation) {
        if (evaluation >= MATE_EVALUATION_THRESHOLD) {
            return 1.0;
        }
        if (evaluation <= -MATE_EVALUATION_THRESHOLD) {
            return 0.0;
        }

        double result = 0.5 + Math.atan(Math.tan(Math.PI / 10d) * evaluation) / Math.PI;
        return Math.max(0.0, Math.min(1.0, result));
    }
}
