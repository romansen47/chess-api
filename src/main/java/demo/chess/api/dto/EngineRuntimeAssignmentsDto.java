package demo.chess.api.dto;

/**
 * Runtime-only engine profile overrides.
 *
 * <p>A {@code null} field means that the corresponding persistent default
 * profile remains effective.</p>
 */
public class EngineRuntimeAssignmentsDto {

    private String whitePlayerProfileId;
    private String blackPlayerProfileId;
    private String evaluationProfileId;

    /** Creates an empty assignment DTO for serialization frameworks. */
    public EngineRuntimeAssignmentsDto() {
    }

    /**
     * Creates a runtime assignment snapshot.
     *
     * @param whitePlayerProfileId White-player override, or {@code null}
     * @param blackPlayerProfileId Black-player override, or {@code null}
     * @param evaluationProfileId live-evaluation override, or {@code null}
     */
    public EngineRuntimeAssignmentsDto(
            String whitePlayerProfileId,
            String blackPlayerProfileId,
            String evaluationProfileId) {
        this.whitePlayerProfileId = whitePlayerProfileId;
        this.blackPlayerProfileId = blackPlayerProfileId;
        this.evaluationProfileId = evaluationProfileId;
    }

    /** @return White-player runtime profile override, or {@code null} */
    public String getWhitePlayerProfileId() {
        return whitePlayerProfileId;
    }

    /** @param whitePlayerProfileId White-player runtime profile override */
    public void setWhitePlayerProfileId(String whitePlayerProfileId) {
        this.whitePlayerProfileId = whitePlayerProfileId;
    }

    /** @return Black-player runtime profile override, or {@code null} */
    public String getBlackPlayerProfileId() {
        return blackPlayerProfileId;
    }

    /** @param blackPlayerProfileId Black-player runtime profile override */
    public void setBlackPlayerProfileId(String blackPlayerProfileId) {
        this.blackPlayerProfileId = blackPlayerProfileId;
    }

    /** @return live-evaluation runtime profile override, or {@code null} */
    public String getEvaluationProfileId() {
        return evaluationProfileId;
    }

    /** @param evaluationProfileId live-evaluation runtime profile override */
    public void setEvaluationProfileId(String evaluationProfileId) {
        this.evaluationProfileId = evaluationProfileId;
    }
}
