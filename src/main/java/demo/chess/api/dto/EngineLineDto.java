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

    public EngineLineDto() {
    }

    public EngineLineDto(double eval, int depth, String moves) {
        this(eval, depth, null, moves, List.of());
    }

    public EngineLineDto(double eval, int depth, String moves, List<String> positions) {
        this(eval, depth, null, moves, positions);
    }

    public EngineLineDto(double eval, int depth, Integer mateDistance, String moves) {
        this(eval, depth, mateDistance, moves, List.of());
    }

    public EngineLineDto(double eval, int depth, Integer mateDistance, String moves, List<String> positions) {
        this.eval = eval;
        this.depth = depth;
        this.mateDistance = mateDistance;
        this.moves = moves;
        this.positions = positions != null ? positions : List.of();
    }

    public double getEval() {
        return eval;
    }

    public void setEval(double eval) {
        this.eval = eval;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public Integer getMateDistance() {
        return mateDistance;
    }

    public void setMateDistance(Integer mateDistance) {
        this.mateDistance = mateDistance;
    }

    public String getMoves() {
        return moves;
    }

    public void setMoves(String moves) {
        this.moves = moves;
    }

    public List<String> getPositions() {
        return positions;
    }

    public void setPositions(List<String> positions) {
        this.positions = positions != null ? positions : List.of();
    }
}
