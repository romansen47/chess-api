package demo.chess.api.dto;

/** Mutable update payload for editable opening metadata. */
public class OpeningUpdateDto {

    private String name;

    public OpeningUpdateDto() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
