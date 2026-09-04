package demo.chess.api.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class ManagedEngineConfigDto {

    private String id;
    private String name;
    private String type;
    private String engine;
    private String engineName;
    private String engineAuthor;
    private int depth;
    private int moveTimeSeconds;
    private Map<String, UciOptionDto> options = new LinkedHashMap<>();

    /**
     * Creates a new ManagedEngineConfigDto instance.
     */
    public ManagedEngineConfigDto() {
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
     * Returns the type.
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the engine.
     * @return the engine
     */
    public String getEngine() {
        return engine;
    }

    /**
     * Sets the engine.
     * @param engine the engine
     */
    public void setEngine(String engine) {
        this.engine = engine;
    }

    /**
     * Returns the engine name.
     * @return the engine name
     */
    public String getEngineName() {
        return engineName;
    }

    /**
     * Sets the engine name.
     * @param engineName the engine name
     */
    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    /**
     * Returns the engine author.
     * @return the engine author
     */
    public String getEngineAuthor() {
        return engineAuthor;
    }

    /**
     * Sets the engine author.
     * @param engineAuthor the engine author
     */
    public void setEngineAuthor(String engineAuthor) {
        this.engineAuthor = engineAuthor;
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
