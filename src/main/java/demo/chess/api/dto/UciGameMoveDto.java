package demo.chess.api.dto;

public class UciGameMoveDto {

    private int ply;
    private String uci;
    private String san;
    private String position;

    public UciGameMoveDto() {
    }

    public UciGameMoveDto(int ply, String uci, String san, String position) {
        this.ply = ply;
        this.uci = uci;
        this.san = san;
        this.position = position;
    }

    public int getPly() {
        return ply;
    }

    public void setPly(int ply) {
        this.ply = ply;
    }

    public String getUci() {
        return uci;
    }

    public void setUci(String uci) {
        this.uci = uci;
    }

    public String getSan() {
        return san;
    }

    public void setSan(String san) {
        this.san = san;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}
