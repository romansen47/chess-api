package demo.chess.api.dto;

/**
 * Legacy slot DTO kept only so older clients can be migrated independently.
 * New game/player assignment is based on engineConfigId.
 */
@Deprecated
public class UciEngineSlotSettingsDto {

    private String displayName;
    private String enginePath;
    private String engineConfigId;
    private UciEngineSettingsDto settings = new UciEngineSettingsDto();

    public UciEngineSlotSettingsDto() {
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEnginePath() {
        return enginePath;
    }

    public void setEnginePath(String enginePath) {
        this.enginePath = enginePath;
    }

    public String getEngineConfigId() {
        return engineConfigId;
    }

    public void setEngineConfigId(String engineConfigId) {
        this.engineConfigId = engineConfigId;
    }

    public UciEngineSettingsDto getSettings() {
        if (settings == null) {
            settings = new UciEngineSettingsDto();
        }
        return settings;
    }

    public void setSettings(UciEngineSettingsDto settings) {
        this.settings = settings != null ? settings : new UciEngineSettingsDto();
    }
}
