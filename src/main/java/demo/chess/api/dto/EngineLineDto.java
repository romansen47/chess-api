package demo.chess.api.dto;

import java.util.List;

public class EngineLineDto {

    /**
     * Bewertung der Variante (aus Sicht von Weiß, z.B. +0.7).
     */
    private double eval;

    /**
     * Suchtiefe, mit der diese Variante berechnet wurde.
     */
    private int depth;

    /**
     * Mattdistanz der Engine in Zügen. null bedeutet normale cp-Bewertung.
     *
     * Die mattsetzende Farbe ergibt sich weiterhin aus eval: positiver Wert =
     * Weiß, negativer Wert = Schwarz. Bei mate 0 ist die Distanz 0.
     */
    private Integer mateDistance;

    /**
     * Zugfolge in Anzeige-Notation, z.B. "e4 e5 ♘f3".
     */
    private String moves;

    /**
     * Brettstellungen der Variante als 64-Zeichen-Strings.
     *
     * positions[0] ist die Ausgangsstellung der Variante, danach folgt je ein
     * Eintrag nach jedem angewendeten Engine-Zug.
     */
    private List<String> positions = List.of();

    /**
     * Creates a new EngineLineDto instance.
     */
    public EngineLineDto() {
    }

    /**
     * Creates a new EngineLineDto instance.
     * @param eval the eval
     * @param depth the depth
     * @param moves the moves
     */
    public EngineLineDto(double eval, int depth, String moves) {
        this(eval, depth, null, moves, List.of());
    }

    /**
     * Creates a new EngineLineDto instance.
     * @param eval the eval
     * @param depth the depth
     * @param moves the moves
     * @param positions the positions
     */
    public EngineLineDto(double eval, int depth, String moves, List<String> positions) {
        this(eval, depth, null, moves, positions);
    }

    /**
     * Creates a new EngineLineDto instance.
     * @param eval the eval
     * @param depth the depth
     * @param mateDistance the mate distance
     * @param moves the moves
     */
    public EngineLineDto(double eval, int depth, Integer mateDistance, String moves) {
        this(eval, depth, mateDistance, moves, List.of());
    }

    /**
     * Creates a new EngineLineDto instance.
     * @param eval the eval
     * @param depth the depth
     * @param mateDistance the mate distance
     * @param moves the moves
     * @param positions the positions
     */
    public EngineLineDto(double eval, int depth, Integer mateDistance, String moves, List<String> positions) {
        this.eval = eval;
        this.depth = depth;
        this.mateDistance = mateDistance;
        this.moves = moves;
        this.positions = positions != null ? positions : List.of();
    }

    /**
     * Returns the eval.
     * @return the eval
     */
    public double getEval() {
        return eval;
    }

    /**
     * Sets the eval.
     * @param eval the eval
     */
    public void setEval(double eval) {
        this.eval = eval;
    }

    /**
     * Returns the depth.
     * @return the depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Sets the depth.
     * @param depth the depth
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * Returns the mate distance.
     * @return the mate distance
     */
    public Integer getMateDistance() {
        return mateDistance;
    }

    /**
     * Sets the mate distance.
     * @param mateDistance the mate distance
     */
    public void setMateDistance(Integer mateDistance) {
        this.mateDistance = mateDistance;
    }

    /**
     * Returns the moves.
     * @return the moves
     */
    public String getMoves() {
        return moves;
    }

    /**
     * Sets the moves.
     * @param moves the moves
     */
    public void setMoves(String moves) {
        this.moves = moves;
    }

    /**
     * Returns the positions.
     * @return the positions
     */
    public List<String> getPositions() {
        return positions;
    }

    /**
     * Sets the positions.
     * @param positions the positions
     */
    public void setPositions(List<String> positions) {
        this.positions = positions != null ? positions : List.of();
    }
}
