package demo.chess.api.dto;

/**
 * REST-/UI-Sicht auf die Engine-Einstellungen.
 *
 * Die technische Engine-Identität ist der enginePath des jeweiligen Slots.
 * Stockfish bleibt serverseitig Default und Fallback, wenn ein Slot keinen
 * Pfad liefert oder eine konfigurierte Engine nicht gestartet werden kann.
 */
public class EngineSettingsDto {

    private UciEngineSlotSettingsDto whitePlayer;
    private UciEngineSlotSettingsDto blackPlayer;
    private UciEngineSlotSettingsDto evaluation;

    private long version;
    private long whitePlayerVersion;
    private long blackPlayerVersion;
    private long evaluationVersion;

    /**
     * Creates a new EngineSettingsDto instance.
     */
    public EngineSettingsDto() {
        this.whitePlayer = new UciEngineSlotSettingsDto();
        this.blackPlayer = new UciEngineSlotSettingsDto();
        this.evaluation = new UciEngineSlotSettingsDto();
    }

    /**
     * Creates a new EngineSettingsDto instance.
     * @param whitePlayer the white player
     * @param blackPlayer the black player
     * @param evaluation the evaluation
     * @param version the version
     * @param whitePlayerVersion the white player version
     * @param blackPlayerVersion the black player version
     * @param evaluationVersion the evaluation version
     */
    public EngineSettingsDto(UciEngineSlotSettingsDto whitePlayer,
            UciEngineSlotSettingsDto blackPlayer,
            UciEngineSlotSettingsDto evaluation,
            long version,
            long whitePlayerVersion,
            long blackPlayerVersion,
            long evaluationVersion) {
        this.whitePlayer = whitePlayer != null ? whitePlayer : new UciEngineSlotSettingsDto();
        this.blackPlayer = blackPlayer != null ? blackPlayer : new UciEngineSlotSettingsDto();
        this.evaluation = evaluation != null ? evaluation : new UciEngineSlotSettingsDto();
        this.version = version;
        this.whitePlayerVersion = whitePlayerVersion;
        this.blackPlayerVersion = blackPlayerVersion;
        this.evaluationVersion = evaluationVersion;
    }

    /**
     * Returns the white player.
     * @return the white player
     */
    public UciEngineSlotSettingsDto getWhitePlayer() {
        if (whitePlayer == null) {
            whitePlayer = new UciEngineSlotSettingsDto();
        }
        return whitePlayer;
    }

    /**
     * Sets the white player.
     * @param whitePlayer the white player
     */
    public void setWhitePlayer(UciEngineSlotSettingsDto whitePlayer) {
        this.whitePlayer = whitePlayer != null ? whitePlayer : new UciEngineSlotSettingsDto();
    }

    /**
     * Returns the black player.
     * @return the black player
     */
    public UciEngineSlotSettingsDto getBlackPlayer() {
        if (blackPlayer == null) {
            blackPlayer = new UciEngineSlotSettingsDto();
        }
        return blackPlayer;
    }

    /**
     * Sets the black player.
     * @param blackPlayer the black player
     */
    public void setBlackPlayer(UciEngineSlotSettingsDto blackPlayer) {
        this.blackPlayer = blackPlayer != null ? blackPlayer : new UciEngineSlotSettingsDto();
    }

    /**
     * Returns the evaluation.
     * @return the evaluation
     */
    public UciEngineSlotSettingsDto getEvaluation() {
        if (evaluation == null) {
            evaluation = new UciEngineSlotSettingsDto();
        }
        return evaluation;
    }

    /**
     * Sets the evaluation.
     * @param evaluation the evaluation
     */
    public void setEvaluation(UciEngineSlotSettingsDto evaluation) {
        this.evaluation = evaluation != null ? evaluation : new UciEngineSlotSettingsDto();
    }

    /**
     * Returns the version.
     * @return the version
     */
    public long getVersion() {
        return version;
    }

    /**
     * Sets the version.
     * @param version the version
     */
    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * Returns the white player version.
     * @return the white player version
     */
    public long getWhitePlayerVersion() {
        return whitePlayerVersion;
    }

    /**
     * Sets the white player version.
     * @param whitePlayerVersion the white player version
     */
    public void setWhitePlayerVersion(long whitePlayerVersion) {
        this.whitePlayerVersion = whitePlayerVersion;
    }

    /**
     * Returns the black player version.
     * @return the black player version
     */
    public long getBlackPlayerVersion() {
        return blackPlayerVersion;
    }

    /**
     * Sets the black player version.
     * @param blackPlayerVersion the black player version
     */
    public void setBlackPlayerVersion(long blackPlayerVersion) {
        this.blackPlayerVersion = blackPlayerVersion;
    }

    /**
     * Returns the evaluation version.
     * @return the evaluation version
     */
    public long getEvaluationVersion() {
        return evaluationVersion;
    }

    /**
     * Sets the evaluation version.
     * @param evaluationVersion the evaluation version
     */
    public void setEvaluationVersion(long evaluationVersion) {
        this.evaluationVersion = evaluationVersion;
    }
}
