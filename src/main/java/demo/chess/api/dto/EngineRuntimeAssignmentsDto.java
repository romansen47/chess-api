package demo.chess.api.dto;

public class EngineRuntimeAssignmentsDto {

    private String whitePlayerProfileId;
    private String blackPlayerProfileId;
    private String evaluationProfileId;

    public EngineRuntimeAssignmentsDto() {
    }

    public EngineRuntimeAssignmentsDto(
            String whitePlayerProfileId,
            String blackPlayerProfileId,
            String evaluationProfileId) {
        this.whitePlayerProfileId = whitePlayerProfileId;
        this.blackPlayerProfileId = blackPlayerProfileId;
        this.evaluationProfileId = evaluationProfileId;
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
}
