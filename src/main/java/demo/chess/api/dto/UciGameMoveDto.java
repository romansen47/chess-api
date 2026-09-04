package demo.chess.api.dto;

public class UciGameMoveDto {

    private int ply;
    private String uci;
    private String san;
    private String position;

    /**
     * Creates a new UciGameMoveDto instance.
     */
    public UciGameMoveDto() {
    }

    /**
     * Creates a new UciGameMoveDto instance.
     * @param ply the ply
     * @param uci the uci
     * @param san the san
     * @param position the position
     */
    public UciGameMoveDto(int ply, String uci, String san, String position) {
        this.ply = ply;
        this.uci = uci;
        this.san = san;
        this.position = position;
    }

    /**
     * Returns the ply.
     * @return the ply
     */
    public int getPly() {
        return ply;
    }

    /**
     * Sets the ply.
     * @param ply the ply
     */
    public void setPly(int ply) {
        this.ply = ply;
    }

    /**
     * Returns the uci.
     * @return the uci
     */
    public String getUci() {
        return uci;
    }

    /**
     * Sets the uci.
     * @param uci the uci
     */
    public void setUci(String uci) {
        this.uci = uci;
    }

    /**
     * Returns the san.
     * @return the san
     */
    public String getSan() {
        return san;
    }

    /**
     * Sets the san.
     * @param san the san
     */
    public void setSan(String san) {
        this.san = san;
    }

    /**
     * Returns the position.
     * @return the position
     */
    public String getPosition() {
        return position;
    }

    /**
     * Sets the position.
     * @param position the position
     */
    public void setPosition(String position) {
        this.position = position;
    }
}
