package demo.chess.api.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Reusable engine profile: one engine definition plus concrete UCI option values.
 *
 * Player/evaluation/deep-analysis are deliberately not profile properties. Those
 * are use-case assignments handled separately.
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

    /**
     * Creates a new EngineProfileDto instance.
     */
    public EngineProfileDto() {
    }

    /**
     * Returns the id.
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id.
     * @param id the id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the name.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the engine id.
     * @return the engine id
     */
    public String getEngineId() {
        return engineId;
    }

    /**
     * Sets the engine id.
     * @param engineId the engine id
     */
    public void setEngineId(String engineId) {
        this.engineId = engineId;
    }

    /**
     * Returns the option values.
     * @return the option values
     */
    public Map<String, String> getOptionValues() {
        if (optionValues == null) {
            optionValues = new LinkedHashMap<>();
        }
        return optionValues;
    }

    /**
     * Sets the option values.
     * @param optionValues the option values
     */
    public void setOptionValues(Map<String, String> optionValues) {
        this.optionValues = optionValues != null
                ? new LinkedHashMap<>(optionValues)
                : new LinkedHashMap<>();
    }

    /**
     * Returns the legacy type.
     * @return the legacy type
     */
    @JsonIgnore
    public String getLegacyType() {
        return legacyType;
    }

    /**
     * Returns the legacy depth.
     * @return the legacy depth
     */
    @JsonIgnore
    public Integer getLegacyDepth() {
        return legacyDepth;
    }

    /**
     * Returns the legacy move time seconds.
     * @return the legacy move time seconds
     */
    @JsonIgnore
    public Integer getLegacyMoveTimeSeconds() {
        return legacyMoveTimeSeconds;
    }
}
