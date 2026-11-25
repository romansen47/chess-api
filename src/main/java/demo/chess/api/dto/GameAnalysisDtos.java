package demo.chess.api.dto;

import java.util.List;

/** Transport types for completed game-analysis runs and their comparison. */
public final class GameAnalysisDtos {

    private GameAnalysisDtos() {
    }

    public record RunSummary(
            long id,
            String engineName,
            String engineProfileId,
            int requestedDepth,
            int moveTimeSeconds,
            int multiPv,
            int positionCount,
            int maximumDepth,
            String createdAt,
            boolean active) {
    }

    public record Line(
            int rank,
            double evaluation,
            double normalizedScore,
            int depth,
            Integer mateDistance,
            List<String> uciMoves) {
    }

    public record Position(
            int ply,
            double evaluation,
            double normalizedScore,
            int depth,
            Integer mateDistance,
            List<Line> lines) {
    }

    public record Run(
            RunSummary summary,
            List<Position> positions) {
    }

    public record PositionComparison(
            int ply,
            Position left,
            Position right,
            boolean differentBestMove,
            boolean differentContinuation,
            boolean mateDisagreement,
            double normalizedScoreGap,
            int commonPrefixLength,
            Integer leftBestMoveRankOnRight,
            Integer rightBestMoveRankOnLeft) {
    }

    public record Comparison(
            RunSummary left,
            RunSummary right,
            List<PositionComparison> positions) {
    }
}
