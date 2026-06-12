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

    public AnalysisProfilePointDto() {
    }

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

    public int getPly() {
        return ply;
    }

    public void setPly(int ply) {
        this.ply = ply;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSan() {
        return san;
    }

    public void setSan(String san) {
        this.san = san;
    }

    public double getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(double evaluation) {
        this.evaluation = evaluation;
    }

    public double getBar() {
        return bar;
    }

    public void setBar(double bar) {
        this.bar = bar;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public List<EngineLineDto> getLines() {
        return lines;
    }

    public void setLines(List<EngineLineDto> lines) {
        this.lines = lines != null ? lines : List.of();
    }
}
