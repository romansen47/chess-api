package demo.chess.api.dto;

public class EngineConfigInspectRequestDto {

    private String engine;
    private String name;
    private String type;

    public EngineConfigInspectRequestDto() {
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
