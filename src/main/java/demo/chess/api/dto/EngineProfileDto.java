package demo.chess.api.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import demo.chess.definitions.engines.UciSystemOptions;

/**
 * Reusable engine profile: one engine definition plus concrete user-managed
 * UCI option values.
 *
 * <p>Player/evaluation/deep-analysis are deliberately not profile properties.
 * Those are use-case assignments handled separately. Protocol state derived
 * from the current game is not a profile property either: system-managed
 * options such as {@code UCI_Chess960} are removed when profile values enter
 * the DTO. This also migrates legacy persisted profiles transparently.</p>
 */
public class EngineProfileDto {

    private String id;
    private String name;
    private String engineId;
    private Map<String, String> optionValues = new LinkedHashMap<>();

    // Legacy profile fields. They are accepted when reading the previous store
    // format, but are never emitted by the new API/model.
    @JsonProperty(value = "type", access = JsonProperty.Access.WRITE_ONLY)
    private String legacyType;

    @JsonProperty(value = "depth", access = JsonProperty.Access.WRITE_ONLY)
    private Integer legacyDepth;

    @JsonProperty(value = "moveTimeSeconds", access = JsonProperty.Access.WRITE_ONLY)
    private Integer legacyMoveTimeSeconds;

    /** Creates a new EngineProfileDto instance. */
    public EngineProfileDto() {
    }

    /** @return profile id */
    public String getId() {
        return id;
    }

    /** @param id profile id */
    public void setId(String id) {
        this.id = id;
    }

    /** @return profile display name */
    public String getName() {
        return name;
    }

    /** @param name profile display name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return referenced engine definition id */
    public String getEngineId() {
        return engineId;
    }

    /** @param engineId referenced engine definition id */
    public void setEngineId(String engineId) {
        this.engineId = engineId;
    }

    /**
     * Returns user-managed profile values.
     * @return mutable profile value map used by Jackson and service mapping
     */
    public Map<String, String> getOptionValues() {
        if (optionValues == null) {
            optionValues = new LinkedHashMap<>();
        }
        return optionValues;
    }

    /**
     * Sets user-managed profile values and drops protocol state that is owned by
     * runtime game context.
     *
     * @param optionValues incoming profile values
     */
    public void setOptionValues(Map<String, String> optionValues) {
        LinkedHashMap<String, String> filtered = new LinkedHashMap<>();
        if (optionValues != null) {
            for (Map.Entry<String, String> entry : optionValues.entrySet()) {
                if (!UciSystemOptions.isSystemManaged(entry.getKey())) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
        }
        this.optionValues = filtered;
    }

    /** @return legacy profile type used only while migrating older stores */
    @JsonIgnore
    public String getLegacyType() {
        return legacyType;
    }

    /** @return legacy depth used only while migrating older stores */
    @JsonIgnore
    public Integer getLegacyDepth() {
        return legacyDepth;
    }

    /** @return legacy move time used only while migrating older stores */
    @JsonIgnore
    public Integer getLegacyMoveTimeSeconds() {
        return legacyMoveTimeSeconds;
    }
}
