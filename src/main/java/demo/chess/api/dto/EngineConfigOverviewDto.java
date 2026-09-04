package demo.chess.api.dto;

import java.util.ArrayList;
import java.util.List;

public class EngineConfigOverviewDto {

    private List<EngineDefinitionDto> engines = new ArrayList<>();
    private List<EngineProfileDto> profiles = new ArrayList<>();
    private EngineProfileAssignmentsDto defaults = new EngineProfileAssignmentsDto();
    private String fallbackProfileId;
    private long version;

    /**
     * Creates a new EngineConfigOverviewDto instance.
     */
    public EngineConfigOverviewDto() {
    }

    /**
     * Creates a new EngineConfigOverviewDto instance.
     * @param engines the engines
     * @param profiles the profiles
     * @param defaults the defaults
     * @param fallbackProfileId the fallback profile id
     * @param version the version
     */
    public EngineConfigOverviewDto(
            List<EngineDefinitionDto> engines,
            List<EngineProfileDto> profiles,
            EngineProfileAssignmentsDto defaults,
            String fallbackProfileId,
            long version) {
        this.engines = engines != null ? new ArrayList<>(engines) : new ArrayList<>();
        this.profiles = profiles != null ? new ArrayList<>(profiles) : new ArrayList<>();
        this.defaults = defaults != null ? defaults : new EngineProfileAssignmentsDto();
        this.fallbackProfileId = fallbackProfileId;
        this.version = version;
    }

    /**
     * Returns the engines.
     * @return the engines
     */
    public List<EngineDefinitionDto> getEngines() {
        return engines;
    }

    /**
     * Sets the engines.
     * @param engines the engines
     */
    public void setEngines(List<EngineDefinitionDto> engines) {
        this.engines = engines != null ? new ArrayList<>(engines) : new ArrayList<>();
    }

    /**
     * Returns the profiles.
     * @return the profiles
     */
    public List<EngineProfileDto> getProfiles() {
        return profiles;
    }

    /**
     * Sets the profiles.
     * @param profiles the profiles
     */
    public void setProfiles(List<EngineProfileDto> profiles) {
        this.profiles = profiles != null ? new ArrayList<>(profiles) : new ArrayList<>();
    }

    /**
     * Returns the defaults.
     * @return the defaults
     */
    public EngineProfileAssignmentsDto getDefaults() {
        if (defaults == null) {
            defaults = new EngineProfileAssignmentsDto();
        }
        return defaults;
    }

    /**
     * Sets the defaults.
     * @param defaults the defaults
     */
    public void setDefaults(EngineProfileAssignmentsDto defaults) {
        this.defaults = defaults != null ? defaults : new EngineProfileAssignmentsDto();
    }

    /**
     * Returns the fallback profile id.
     * @return the fallback profile id
     */
    public String getFallbackProfileId() {
        return fallbackProfileId;
    }

    /**
     * Sets the fallback profile id.
     * @param fallbackProfileId the fallback profile id
     */
    public void setFallbackProfileId(String fallbackProfileId) {
        this.fallbackProfileId = fallbackProfileId;
    }

    /**
     * Returns the version.
     * @return the version
     */
    public long getVersion() {
        return version;
    }

    /**
     * Sets the version.
     * @param version the version
     */
    public void setVersion(long version) {
        this.version = version;
    }
}
