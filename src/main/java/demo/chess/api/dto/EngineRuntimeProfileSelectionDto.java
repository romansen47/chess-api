package demo.chess.api.dto;

public class EngineRuntimeProfileSelectionDto {

    private String profileId;

    public EngineRuntimeProfileSelectionDto() {
    }

    public EngineRuntimeProfileSelectionDto(String profileId) {
        this.profileId = profileId;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }
}
