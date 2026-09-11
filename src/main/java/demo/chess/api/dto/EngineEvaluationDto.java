package demo.chess.api.dto;

import java.util.List;

public class EngineEvaluationDto {

    /**
     * Rohbewertung der Stellung (in Figurenwerten, z.B. +0.7 = besser für Weiß).
     */
    private double eval;

    /**
     * Auf [0,1] gemappte Bewertung für eine Eval-Bar (0 = schwarz, 1 = weiß).
     */
    private double bar;

    /**
     * UCI-Name der aktuell verwendeten Bewertungsengine.
     */
    private String engineName;

    /**
     * Mehrere beste Varianten der Engine (MultiPV).
     */
    private List<EngineLineDto> lines;

    /**
     * Whether the live historical-move assessment has a usable search result.
     */
    private boolean moveAnnotationReady;

    /**
     * Current search depth used for the live move annotation.
     */
    private int moveAnnotationDepth;

    /**
     * Current live annotation for the selected historical move.
     * Null is also a valid ready result and means that the live engine does
     * not currently assign one of the configured annotation symbols.
     */
    private MoveAnnotationDto moveAnnotation;

    /**
     * Creates a new EngineEvaluationDto instance.
     */
    public EngineEvaluationDto() {
    }

    /**
     * Creates a new EngineEvaluationDto instance.
     * @param eval the eval
     * @param bar the bar
     * @param lines the lines
     */
    public EngineEvaluationDto(double eval, double bar, List<EngineLineDto> lines) {
        this.eval = eval;
        this.bar = bar;
        this.lines = lines;
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
     * Returns the bar.
     * @return the bar
     */
    public double getBar() {
        return bar;
    }

    /**
     * Sets the bar.
     * @param bar the bar
     */
    public void setBar(double bar) {
        this.bar = bar;
    }

    /**
     * Returns the engine name.
     * @return the engine name
     */
    public String getEngineName() {
        return engineName;
    }

    /**
     * Sets the engine name.
     * @param engineName the engine name
     */
    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    /**
     * Returns the lines.
     * @return the lines
     */
    public List<EngineLineDto> getLines() {
        return lines;
    }

    /**
     * Sets the lines.
     * @param lines the lines
     */
    public void setLines(List<EngineLineDto> lines) {
        this.lines = lines;
    }

    public boolean isMoveAnnotationReady() {
        return moveAnnotationReady;
    }

    public void setMoveAnnotationReady(boolean moveAnnotationReady) {
        this.moveAnnotationReady = moveAnnotationReady;
    }

    public int getMoveAnnotationDepth() {
        return moveAnnotationDepth;
    }

    public void setMoveAnnotationDepth(int moveAnnotationDepth) {
        this.moveAnnotationDepth = moveAnnotationDepth;
    }

    public MoveAnnotationDto getMoveAnnotation() {
        return moveAnnotation;
    }

    public void setMoveAnnotation(MoveAnnotationDto moveAnnotation) {
        this.moveAnnotation = moveAnnotation;
    }
}
