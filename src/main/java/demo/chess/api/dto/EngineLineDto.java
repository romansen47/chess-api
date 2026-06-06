package demo.chess.api.dto;

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
     * Zugfolge in UCI-Notation, z.B. "e2e4 e7e5 g1f3".
     */
    private String moves;

    public EngineLineDto() {
    }

    public EngineLineDto(double eval, int depth, String moves) {
        this.eval = eval;
        this.depth = depth;
        this.moves = moves;
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

    public String getMoves() {
        return moves;
    }

    public void setMoves(String moves) {
        this.moves = moves;
    }
}
