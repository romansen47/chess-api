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

    public EngineEvaluationDto() {
    }

    public EngineEvaluationDto(double eval, double bar, List<EngineLineDto> lines) {
        this.eval = eval;
        this.bar = bar;
        this.lines = lines;
    }

    public double getEval() {
        return eval;
    }

    public void setEval(double eval) {
        this.eval = eval;
    }

    public double getBar() {
        return bar;
    }

    public void setBar(double bar) {
        this.bar = bar;
    }

    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public List<EngineLineDto> getLines() {
        return lines;
    }

    public void setLines(List<EngineLineDto> lines) {
        this.lines = lines;
    }
}
