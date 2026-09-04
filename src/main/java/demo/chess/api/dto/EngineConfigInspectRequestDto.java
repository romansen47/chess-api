package demo.chess.api.dto;

public class EngineConfigInspectRequestDto {

    private String engine;
    private String name;
    private String type;

    /**
     * Creates a new EngineConfigInspectRequestDto instance.
     */
    public EngineConfigInspectRequestDto() {
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
}
