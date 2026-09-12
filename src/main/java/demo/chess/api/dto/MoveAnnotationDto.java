package demo.chess.api.dto;

/**
 * Display-facing representation of one move annotation calculated by the
 * chess core from finite DeepAnalysis data.
 */
public class MoveAnnotationDto {

    private String symbol;
    private String kind;
    private Double winChanceLoss;
    private double bestEvaluation;
    private Double secondBestEvaluation;

    private String extraordinaryReason;
    private Double materialInvestment;
    private String sacrificeType;

    private Integer earlyDepth;
    private Integer earlyRank;
    private Integer finalDepth;
    private Integer finalRank;
    private Boolean givesCheck;
    private Double earlyRegret;
    private Double earlyStrength;
    private Double finalStrength;

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

    public Double getWinChanceLoss() {
        return winChanceLoss;
    }

    public void setWinChanceLoss(Double winChanceLoss) {
        this.winChanceLoss = winChanceLoss;
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

    public String getExtraordinaryReason() {
        return extraordinaryReason;
    }

    public void setExtraordinaryReason(String extraordinaryReason) {
        this.extraordinaryReason = extraordinaryReason;
    }

    public Double getMaterialInvestment() {
        return materialInvestment;
    }

    public void setMaterialInvestment(Double materialInvestment) {
        this.materialInvestment = materialInvestment;
    }

    public String getSacrificeType() {
        return sacrificeType;
    }

    public void setSacrificeType(String sacrificeType) {
        this.sacrificeType = sacrificeType;
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

    public Boolean getGivesCheck() {
        return givesCheck;
    }

    public void setGivesCheck(Boolean givesCheck) {
        this.givesCheck = givesCheck;
    }

    public Double getEarlyRegret() {
        return earlyRegret;
    }

    public void setEarlyRegret(Double earlyRegret) {
        this.earlyRegret = earlyRegret;
    }

    public Double getEarlyStrength() {
        return earlyStrength;
    }

    public void setEarlyStrength(Double earlyStrength) {
        this.earlyStrength = earlyStrength;
    }

    public Double getFinalStrength() {
        return finalStrength;
    }

    public void setFinalStrength(Double finalStrength) {
        this.finalStrength = finalStrength;
    }
}
