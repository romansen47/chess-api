package demo.chess.api.dto;

import java.util.ArrayList;
import java.util.List;

public class UciOptionDto {

    private String type;
    private String defaultValue;
    private String value;
    private Integer min;
    private Integer max;
    private List<String> vars = new ArrayList<>();

    public UciOptionDto() {
    }

    public UciOptionDto(
            String type,
            String defaultValue,
            String value,
            Integer min,
            Integer max,
            List<String> vars) {
        this.type = type;
        this.defaultValue = defaultValue;
        this.value = value;
        this.min = min;
        this.max = max;
        this.vars = vars != null ? new ArrayList<>(vars) : new ArrayList<>();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }

    public Integer getMax() {
        return max;
    }

    public void setMax(Integer max) {
        this.max = max;
    }

    public List<String> getVars() {
        if (vars == null) {
            vars = new ArrayList<>();
        }
        return vars;
    }

    public void setVars(List<String> vars) {
        this.vars = vars != null ? new ArrayList<>(vars) : new ArrayList<>();
    }
}
