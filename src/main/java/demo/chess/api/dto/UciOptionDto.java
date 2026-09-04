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

    /**
     * Creates a new UciOptionDto instance.
     */
    public UciOptionDto() {
    }

    /**
     * Creates a new UciOptionDto instance.
     * @param type the type
     * @param defaultValue the default value
     * @param value the value
     * @param min the min
     * @param max the max
     * @param vars the vars
     */
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
     * Returns the default value.
     * @return the default value
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets the default value.
     * @param defaultValue the default value
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the value.
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value.
     * @param value the value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns the min.
     * @return the min
     */
    public Integer getMin() {
        return min;
    }

    /**
     * Sets the min.
     * @param min the min
     */
    public void setMin(Integer min) {
        this.min = min;
    }

    /**
     * Returns the max.
     * @return the max
     */
    public Integer getMax() {
        return max;
    }

    /**
     * Sets the max.
     * @param max the max
     */
    public void setMax(Integer max) {
        this.max = max;
    }

    /**
     * Returns the vars.
     * @return the vars
     */
    public List<String> getVars() {
        if (vars == null) {
            vars = new ArrayList<>();
        }
        return vars;
    }

    /**
     * Sets the vars.
     * @param vars the vars
     */
    public void setVars(List<String> vars) {
        this.vars = vars != null ? new ArrayList<>(vars) : new ArrayList<>();
    }
}
