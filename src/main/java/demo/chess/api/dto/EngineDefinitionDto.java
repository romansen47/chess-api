package demo.chess.api.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class EngineDefinitionDto {

    private String id;
    private String name;
    private String engine;
    private String engineName;
    private String engineAuthor;
    private Map<String, UciOptionDto> options = new LinkedHashMap<>();

    /**
     * Creates a new EngineDefinitionDto instance.
     */
    public EngineDefinitionDto() {
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
