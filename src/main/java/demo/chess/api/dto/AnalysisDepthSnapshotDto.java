package demo.chess.api.dto;

import java.util.List;

public class AnalysisDepthSnapshotDto {

    private int depth;
    private List<AnalysisDepthCandidateDto> candidates = List.of();

    /**
     * Creates a new AnalysisDepthSnapshotDto instance.
     */
    public AnalysisDepthSnapshotDto() {
    }

    /**
     * Creates a new AnalysisDepthSnapshotDto instance.
     * @param depth UCI search depth
     * @param candidates candidate moves available at this depth
     */
    public AnalysisDepthSnapshotDto(int depth, List<AnalysisDepthCandidateDto> candidates) {
        this.depth = depth;
        this.candidates = candidates != null ? candidates : List.of();
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public List<AnalysisDepthCandidateDto> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<AnalysisDepthCandidateDto> candidates) {
        this.candidates = candidates != null ? candidates : List.of();
    }
}
