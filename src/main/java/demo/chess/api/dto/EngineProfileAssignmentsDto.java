package demo.chess.api.dto;

/**
 * Global default profile assignments for the four engine use cases.
 */
public class EngineProfileAssignmentsDto {

    private String whitePlayerProfileId;
    private String blackPlayerProfileId;
    private String evaluationProfileId;
    private String deepAnalysisProfileId;

    public EngineProfileAssignmentsDto() {
    }

    public EngineProfileAssignmentsDto(
            String whitePlayerProfileId,
            String blackPlayerProfileId,
            String evaluationProfileId,
            String deepAnalysisProfileId) {
        this.whitePlayerProfileId = whitePlayerProfileId;
        this.blackPlayerProfileId = blackPlayerProfileId;
        this.evaluationProfileId = evaluationProfileId;
        this.deepAnalysisProfileId = deepAnalysisProfileId;
    }

    public String getWhitePlayerProfileId() {
        return whitePlayerProfileId;
    }

    public void setWhitePlayerProfileId(String whitePlayerProfileId) {
        this.whitePlayerProfileId = whitePlayerProfileId;
    }

    public String getBlackPlayerProfileId() {
        return blackPlayerProfileId;
    }

    public void setBlackPlayerProfileId(String blackPlayerProfileId) {
        this.blackPlayerProfileId = blackPlayerProfileId;
    }

    public String getEvaluationProfileId() {
        return evaluationProfileId;
    }

    public void setEvaluationProfileId(String evaluationProfileId) {
        this.evaluationProfileId = evaluationProfileId;
    }

    public String getDeepAnalysisProfileId() {
        return deepAnalysisProfileId;
    }

    public void setDeepAnalysisProfileId(String deepAnalysisProfileId) {
        this.deepAnalysisProfileId = deepAnalysisProfileId;
    }
}
