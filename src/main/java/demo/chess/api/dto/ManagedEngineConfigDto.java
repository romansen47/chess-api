package demo.chess.api.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class ManagedEngineConfigDto {

    private String id;
    private String name;
    private String engine;
    private String engineName;
    private String engineAuthor;
    private int depth;
    private int moveTimeSeconds;
    private Map<String, UciOptionDto> options = new LinkedHashMap<>();

    public ManagedEngineConfigDto() {
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

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public String getEngineAuthor() {
        return engineAuthor;
    }

    public void setEngineAuthor(String engineAuthor) {
        this.engineAuthor = engineAuthor;
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
