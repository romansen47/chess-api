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

    public EngineProfileDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEngineId() {
        return engineId;
    }

    public void setEngineId(String engineId) {
        this.engineId = engineId;
    }

    public Map<String, String> getOptionValues() {
        if (optionValues == null) {
            optionValues = new LinkedHashMap<>();
        }
        return optionValues;
    }

    public void setOptionValues(Map<String, String> optionValues) {
        this.optionValues = optionValues != null
                ? new LinkedHashMap<>(optionValues)
                : new LinkedHashMap<>();
    }

    @JsonIgnore
    public String getLegacyType() {
        return legacyType;
    }

    @JsonIgnore
    public Integer getLegacyDepth() {
        return legacyDepth;
    }

    @JsonIgnore
    public Integer getLegacyMoveTimeSeconds() {
        return legacyMoveTimeSeconds;
    }
}
