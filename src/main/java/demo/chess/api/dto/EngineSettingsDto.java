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

    public EngineSettingsDto() {
        this.whitePlayer = new UciEngineSlotSettingsDto();
        this.blackPlayer = new UciEngineSlotSettingsDto();
        this.evaluation = new UciEngineSlotSettingsDto();
    }

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

    public UciEngineSlotSettingsDto getWhitePlayer() {
        if (whitePlayer == null) {
            whitePlayer = new UciEngineSlotSettingsDto();
        }
        return whitePlayer;
    }

    public void setWhitePlayer(UciEngineSlotSettingsDto whitePlayer) {
        this.whitePlayer = whitePlayer != null ? whitePlayer : new UciEngineSlotSettingsDto();
    }

    public UciEngineSlotSettingsDto getBlackPlayer() {
        if (blackPlayer == null) {
            blackPlayer = new UciEngineSlotSettingsDto();
        }
        return blackPlayer;
    }

    public void setBlackPlayer(UciEngineSlotSettingsDto blackPlayer) {
        this.blackPlayer = blackPlayer != null ? blackPlayer : new UciEngineSlotSettingsDto();
    }

    public UciEngineSlotSettingsDto getEvaluation() {
        if (evaluation == null) {
            evaluation = new UciEngineSlotSettingsDto();
        }
        return evaluation;
    }

    public void setEvaluation(UciEngineSlotSettingsDto evaluation) {
        this.evaluation = evaluation != null ? evaluation : new UciEngineSlotSettingsDto();
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public long getWhitePlayerVersion() {
        return whitePlayerVersion;
    }

    public void setWhitePlayerVersion(long whitePlayerVersion) {
        this.whitePlayerVersion = whitePlayerVersion;
    }

    public long getBlackPlayerVersion() {
        return blackPlayerVersion;
    }

    public void setBlackPlayerVersion(long blackPlayerVersion) {
        this.blackPlayerVersion = blackPlayerVersion;
    }

    public long getEvaluationVersion() {
        return evaluationVersion;
    }

    public void setEvaluationVersion(long evaluationVersion) {
        this.evaluationVersion = evaluationVersion;
    }
}
