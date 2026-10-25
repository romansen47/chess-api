package demo.chess.api.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/** Editable PGN header metadata of the currently selected stored game. */
public class GameMetadataDto {

    private Map<String, String> tags = new LinkedHashMap<>();

    public GameMetadataDto() {
    }

    public GameMetadataDto(Map<String, String> tags) {
        setTags(tags);
    }

    public Map<String, String> getTags() {
        return new LinkedHashMap<>(tags);
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags == null ? new LinkedHashMap<>() : new LinkedHashMap<>(tags);
    }
}
