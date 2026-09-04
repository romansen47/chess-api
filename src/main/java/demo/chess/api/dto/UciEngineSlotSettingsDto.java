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

    /**
     * Creates a new UciEngineSlotSettingsDto instance.
     */
    public UciEngineSlotSettingsDto() {
    }

    /**
     * Returns the display name.
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name.
     * @param displayName the display name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the engine path.
     * @return the engine path
     */
    public String getEnginePath() {
        return enginePath;
    }

    /**
     * Sets the engine path.
     * @param enginePath the engine path
     */
    public void setEnginePath(String enginePath) {
        this.enginePath = enginePath;
    }

    /**
     * Returns the engine config id.
     * @return the engine config id
     */
    public String getEngineConfigId() {
        return engineConfigId;
    }

    /**
     * Sets the engine config id.
     * @param engineConfigId the engine config id
     */
    public void setEngineConfigId(String engineConfigId) {
        this.engineConfigId = engineConfigId;
    }

    /**
     * Returns the settings.
     * @return the settings
     */
    public UciEngineSettingsDto getSettings() {
        if (settings == null) {
            settings = new UciEngineSettingsDto();
        }
        return settings;
    }

    /**
     * Sets the settings.
     * @param settings the settings
     */
    public void setSettings(UciEngineSettingsDto settings) {
        this.settings = settings != null ? settings : new UciEngineSettingsDto();
    }
}
