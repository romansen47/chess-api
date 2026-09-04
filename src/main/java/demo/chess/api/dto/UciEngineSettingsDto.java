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

    /**
     * Creates a new UciEngineSettingsDto instance.
     */
    public UciEngineSettingsDto() {
    }

    /**
     * Returns the depth.
     * @return the depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Sets the depth.
     * @param depth the depth
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * Returns the move time seconds.
     * @return the move time seconds
     */
    public int getMoveTimeSeconds() {
        return moveTimeSeconds;
    }

    /**
     * Sets the move time seconds.
     * @param moveTimeSeconds the move time seconds
     */
    public void setMoveTimeSeconds(int moveTimeSeconds) {
        this.moveTimeSeconds = moveTimeSeconds;
    }

    /**
     * Returns the options.
     * @return the options
     */
    public Map<String, UciOptionDto> getOptions() {
        if (options == null) {
            options = new LinkedHashMap<>();
        }
        return options;
    }

    /**
     * Sets the options.
     * @param options the options
     */
    public void setOptions(Map<String, UciOptionDto> options) {
        this.options = options != null ? new LinkedHashMap<>(options) : new LinkedHashMap<>();
    }
}
