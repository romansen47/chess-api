package demo.chess.api.dto;

/**
 * Display-facing representation of one move annotation calculated by the
 * chess core from finite DeepAnalysis data.
 */
public class MoveAnnotationDto {

    private String symbol;
    private String kind;
    private Double loss;
    private double bestEvaluation;
    private Double secondBestEvaluation;
    private String brilliantReason;
    private Double materialInvestment;
    private Integer earlyDepth;
    private Integer earlyRank;
    private Integer finalDepth;
    private Integer finalRank;

    public MoveAnnotationDto() {
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Double getLoss() {
        return loss;
    }

    public void setLoss(Double loss) {
        this.loss = loss;
    }

    public double getBestEvaluation() {
        return bestEvaluation;
    }

    public void setBestEvaluation(double bestEvaluation) {
        this.bestEvaluation = bestEvaluation;
    }

    public Double getSecondBestEvaluation() {
        return secondBestEvaluation;
    }

    public void setSecondBestEvaluation(Double secondBestEvaluation) {
        this.secondBestEvaluation = secondBestEvaluation;
    }

    public String getBrilliantReason() {
        return brilliantReason;
    }

    public void setBrilliantReason(String brilliantReason) {
        this.brilliantReason = brilliantReason;
    }

    public Double getMaterialInvestment() {
        return materialInvestment;
    }

    public void setMaterialInvestment(Double materialInvestment) {
        this.materialInvestment = materialInvestment;
    }

    public Integer getEarlyDepth() {
        return earlyDepth;
    }

    public void setEarlyDepth(Integer earlyDepth) {
        this.earlyDepth = earlyDepth;
    }

    public Integer getEarlyRank() {
        return earlyRank;
    }

    public void setEarlyRank(Integer earlyRank) {
        this.earlyRank = earlyRank;
    }

    public Integer getFinalDepth() {
        return finalDepth;
    }

    public void setFinalDepth(Integer finalDepth) {
        this.finalDepth = finalDepth;
    }

    public Integer getFinalRank() {
        return finalRank;
    }

    public void setFinalRank(Integer finalRank) {
        this.finalRank = finalRank;
    }
}
