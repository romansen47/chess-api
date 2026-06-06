package demo.chess.api.dto;

import demo.chess.definitions.engines.UciEngineConfig;

/**
 * REST-/UI-Sicht auf einen konkreten Engine-Slot.
 *
 * Ein Slot beschreibt eine Rolle der Engine-Einbindung, z.B. Weiß-Spieler,
 * Schwarz-Spieler oder Bewertungsengine. Jeder Slot besitzt einen eigenen Pfad
 * und eine eigene UCI-Konfiguration.
 */
public class UciEngineSlotSettingsDto {

    private String displayName;
    private String enginePath;
    private UciEngineSettingsDto settings;

    public UciEngineSlotSettingsDto() {
        this.settings = new UciEngineSettingsDto();
    }

    public UciEngineSlotSettingsDto(String displayName, String enginePath, UciEngineConfig config) {
        this.displayName = displayName;
        this.enginePath = enginePath;
        this.settings = new UciEngineSettingsDto(config);
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
