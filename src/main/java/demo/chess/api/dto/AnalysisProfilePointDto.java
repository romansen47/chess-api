package demo.chess.api.dto;

import java.util.List;

public class AnalysisProfilePointDto {

    private int ply;
    private String from;
    private String to;
    private String san;
    private double evaluation;
    private double bar;
    private int depth;
    private List<EngineLineDto> lines = List.of();

    /**
     * Creates a new AnalysisProfilePointDto instance.
     */
    public AnalysisProfilePointDto() {
    }

    /**
     * Creates a new AnalysisProfilePointDto instance.
     * @param ply the ply
     * @param from the from
     * @param to the to
     * @param san the san
     * @param evaluation the evaluation
     * @param bar the bar
     * @param depth the depth
     */
    public AnalysisProfilePointDto(
            int ply,
            String from,
            String to,
            String san,
            double evaluation,
            double bar,
            int depth) {
        this(ply, from, to, san, evaluation, bar, depth, List.of());
    }

    /**
     * Creates a new AnalysisProfilePointDto instance.
     * @param ply the ply
     * @param from the from
     * @param to the to
     * @param san the san
     * @param evaluation the evaluation
     * @param bar the bar
     * @param depth the depth
     * @param lines the lines
     */
    public AnalysisProfilePointDto(
            int ply,
            String from,
            String to,
            String san,
            double evaluation,
            double bar,
            int depth,
            List<EngineLineDto> lines) {
        this.ply = ply;
        this.from = from;
        this.to = to;
        this.san = san;
        this.evaluation = evaluation;
        this.bar = bar;
        this.depth = depth;
        this.lines = lines != null ? lines : List.of();
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
     * Returns the from.
     * @return the from
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the from.
     * @param from the from
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Returns the to.
     * @return the to
     */
    public String getTo() {
        return to;
    }

    /**
     * Sets the to.
     * @param to the to
     */
    public void setTo(String to) {
        this.to = to;
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
     * Returns the evaluation.
     * @return the evaluation
     */
    public double getEvaluation() {
        return evaluation;
    }

    /**
     * Sets the evaluation.
     * @param evaluation the evaluation
     */
    public void setEvaluation(double evaluation) {
        this.evaluation = evaluation;
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
        this.lines = lines != null ? lines : List.of();
    }
}
