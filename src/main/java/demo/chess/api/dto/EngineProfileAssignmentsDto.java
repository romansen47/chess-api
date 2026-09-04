package demo.chess.api.dto;

/**
 * Global default profile assignments for the four engine use cases.
 */
public class EngineProfileAssignmentsDto {

    private String whitePlayerProfileId;
    private String blackPlayerProfileId;
    private String evaluationProfileId;
    private String deepAnalysisProfileId;

    /**
     * Creates a new EngineProfileAssignmentsDto instance.
     */
    public EngineProfileAssignmentsDto() {
    }

    /**
     * Creates a new EngineProfileAssignmentsDto instance.
     * @param whitePlayerProfileId the white player profile id
     * @param blackPlayerProfileId the black player profile id
     * @param evaluationProfileId the evaluation profile id
     * @param deepAnalysisProfileId the deep analysis profile id
     */
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

    /**
     * Returns the white player profile id.
     * @return the white player profile id
     */
    public String getWhitePlayerProfileId() {
        return whitePlayerProfileId;
    }

    /**
     * Sets the white player profile id.
     * @param whitePlayerProfileId the white player profile id
     */
    public void setWhitePlayerProfileId(String whitePlayerProfileId) {
        this.whitePlayerProfileId = whitePlayerProfileId;
    }

    /**
     * Returns the black player profile id.
     * @return the black player profile id
     */
    public String getBlackPlayerProfileId() {
        return blackPlayerProfileId;
    }

    /**
     * Sets the black player profile id.
     * @param blackPlayerProfileId the black player profile id
     */
    public void setBlackPlayerProfileId(String blackPlayerProfileId) {
        this.blackPlayerProfileId = blackPlayerProfileId;
    }

    /**
     * Returns the evaluation profile id.
     * @return the evaluation profile id
     */
    public String getEvaluationProfileId() {
        return evaluationProfileId;
    }

    /**
     * Sets the evaluation profile id.
     * @param evaluationProfileId the evaluation profile id
     */
    public void setEvaluationProfileId(String evaluationProfileId) {
        this.evaluationProfileId = evaluationProfileId;
    }

    /**
     * Returns the deep analysis profile id.
     * @return the deep analysis profile id
     */
    public String getDeepAnalysisProfileId() {
        return deepAnalysisProfileId;
    }

    /**
     * Sets the deep analysis profile id.
     * @param deepAnalysisProfileId the deep analysis profile id
     */
    public void setDeepAnalysisProfileId(String deepAnalysisProfileId) {
        this.deepAnalysisProfileId = deepAnalysisProfileId;
    }
}
