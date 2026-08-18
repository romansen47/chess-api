package demo.chess.api.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class EngineProfileDto {

    private String id;
    private String name;
    private String type;
    private String engineId;
    private int depth;
    private int moveTimeSeconds;
    private Map<String, String> optionValues = new LinkedHashMap<>();

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEngineId() {
        return engineId;
    }

    public void setEngineId(String engineId) {
        this.engineId = engineId;
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
}
