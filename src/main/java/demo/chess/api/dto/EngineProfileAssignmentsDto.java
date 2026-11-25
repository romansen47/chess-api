package demo.chess.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Global default profile assignments for the four engine use cases.
 */
public class EngineProfileAssignmentsDto {

    private String whitePlayerProfileId;
    private String blackPlayerProfileId;
    private String evaluationProfileId;
    private String gameAnalysisProfileId;

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
     * @param gameAnalysisProfileId the game analysis profile id
     */
    public EngineProfileAssignmentsDto(
            String whitePlayerProfileId,
            String blackPlayerProfileId,
            String evaluationProfileId,
            String gameAnalysisProfileId) {
        this.whitePlayerProfileId = whitePlayerProfileId;
        this.blackPlayerProfileId = blackPlayerProfileId;
        this.evaluationProfileId = evaluationProfileId;
        this.gameAnalysisProfileId = gameAnalysisProfileId;
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
     * Returns the game analysis profile id.
     * @return the game analysis profile id
     */
    public String getGameAnalysisProfileId() {
        return gameAnalysisProfileId;
    }

    /**
     * Sets the game analysis profile id.
     * @param gameAnalysisProfileId the game analysis profile id
     */
    public void setGameAnalysisProfileId(String gameAnalysisProfileId) {
        this.gameAnalysisProfileId = gameAnalysisProfileId;
    }

    /** Accepts the persisted field name used before the terminology migration. */
    @JsonProperty(value = "deepAnalysisProfileId", access = JsonProperty.Access.WRITE_ONLY)
    public void setLegacyDeepAnalysisProfileId(String profileId) {
        if (gameAnalysisProfileId == null || gameAnalysisProfileId.isBlank()) {
            gameAnalysisProfileId = profileId;
        }
    }
}
