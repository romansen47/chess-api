package demo.chess.api.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Legacy transport shape kept for source compatibility. New code uses
 * ManagedEngineConfigDto and the /api/engine-configs endpoints.
 */
@Deprecated
public class UciEngineSettingsDto {

    private int depth;
    private int moveTimeSeconds;
    private Map<String, UciOptionDto> options = new LinkedHashMap<>();

    public UciEngineSettingsDto() {
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getMoveTimeSeconds() {
        return moveTimeSeconds;
    }

    public void setMoveTimeSeconds(int moveTimeSeconds) {
        this.moveTimeSeconds = moveTimeSeconds;
    }

    public Map<String, UciOptionDto> getOptions() {
        if (options == null) {
            options = new LinkedHashMap<>();
        }
        return options;
    }

    public void setOptions(Map<String, UciOptionDto> options) {
        this.options = options != null ? new LinkedHashMap<>(options) : new LinkedHashMap<>();
    }
}
