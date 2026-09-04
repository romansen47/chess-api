package demo.chess.api.dto;

public class EngineDefinitionInspectRequestDto {

    private String engine;
    private String name;

    /**
     * Creates a new EngineDefinitionInspectRequestDto instance.
     */
    public EngineDefinitionInspectRequestDto() {
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
}
