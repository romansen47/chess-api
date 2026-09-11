package demo.chess.api.dto;

public class AnalysisDepthCandidateDto {

    private double evaluation;
    private String position;

    /**
     * Creates a new AnalysisDepthCandidateDto instance.
     */
    public AnalysisDepthCandidateDto() {
    }

    /**
     * Creates a new AnalysisDepthCandidateDto instance.
     * @param evaluation evaluation from White's point of view
     * @param position board position after the candidate's first move
     */
    public AnalysisDepthCandidateDto(double evaluation, String position) {
        this.evaluation = evaluation;
        this.position = position;
    }

    public double getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(double evaluation) {
        this.evaluation = evaluation;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}
