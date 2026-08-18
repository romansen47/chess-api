package demo.chess.api.dto;

public class EngineDefinitionInspectRequestDto {

    private String engine;
    private String name;

    public EngineDefinitionInspectRequestDto() {
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
