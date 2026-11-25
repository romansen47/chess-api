package demo.chess.api.service;

/** Maps engine pawn evaluations to a common display score without claiming probability. */
final class NormalizedEvaluationScore {

    private static final double LOGISTIC_FACTOR = 0.00368208;

    private NormalizedEvaluationScore() {
    }

    static double fromPawns(double evaluation) {
        if (evaluation >= 99.0) return 100.0;
        if (evaluation <= -99.0) return 0.0;
        double centipawns = evaluation * 100.0;
        double score = 100.0 / (1.0 + Math.exp(-LOGISTIC_FACTOR * centipawns));
        return Math.round(score * 100.0) / 100.0;
    }
}
