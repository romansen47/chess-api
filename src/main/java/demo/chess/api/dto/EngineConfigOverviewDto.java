package demo.chess.api.dto;

import java.util.ArrayList;
import java.util.List;

public class EngineConfigOverviewDto {

    private List<EngineDefinitionDto> engines = new ArrayList<>();
    private List<EngineProfileDto> profiles = new ArrayList<>();
    private EngineProfileAssignmentsDto defaults = new EngineProfileAssignmentsDto();
    private String fallbackProfileId;
    private long version;

    public EngineConfigOverviewDto() {
    }

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

    public List<EngineDefinitionDto> getEngines() {
        return engines;
    }

    public void setEngines(List<EngineDefinitionDto> engines) {
        this.engines = engines != null ? new ArrayList<>(engines) : new ArrayList<>();
    }

    public List<EngineProfileDto> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<EngineProfileDto> profiles) {
        this.profiles = profiles != null ? new ArrayList<>(profiles) : new ArrayList<>();
    }

    public EngineProfileAssignmentsDto getDefaults() {
        if (defaults == null) {
            defaults = new EngineProfileAssignmentsDto();
        }
        return defaults;
    }

    public void setDefaults(EngineProfileAssignmentsDto defaults) {
        this.defaults = defaults != null ? defaults : new EngineProfileAssignmentsDto();
    }

    public String getFallbackProfileId() {
        return fallbackProfileId;
    }

    public void setFallbackProfileId(String fallbackProfileId) {
        this.fallbackProfileId = fallbackProfileId;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
