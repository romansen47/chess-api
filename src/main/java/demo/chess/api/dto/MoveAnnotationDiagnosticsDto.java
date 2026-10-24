package demo.chess.api.dto;

/**
 * Debug-facing explanation of why a sound best move did or did not qualify
 * for the deep-discovery extraordinary-move signal.
 */
public class MoveAnnotationDiagnosticsDto {

    private boolean extraordinaryCandidate;
    private String deepDiscoveryRejectedBy;
    private int earlyWindowStartDepth;
    private int earlyWindowEndDepth;
    private int earlySampleCount;
    private Integer earlyDepth;
    private Integer earlyRank;
    private int finalDepth;
    private int finalRank;
    private double finalRegret;
    private Double earlyRegret;
    private Double earlyStrength;
    private double finalStrength;
    private Double strengthGain;

    public boolean isExtraordinaryCandidate() {
        return extraordinaryCandidate;
    }

    public void setExtraordinaryCandidate(boolean extraordinaryCandidate) {
        this.extraordinaryCandidate = extraordinaryCandidate;
    }

    public String getDeepDiscoveryRejectedBy() {
        return deepDiscoveryRejectedBy;
    }

    public void setDeepDiscoveryRejectedBy(String deepDiscoveryRejectedBy) {
        this.deepDiscoveryRejectedBy = deepDiscoveryRejectedBy;
    }

    public int getEarlyWindowStartDepth() {
        return earlyWindowStartDepth;
    }

    public void setEarlyWindowStartDepth(int earlyWindowStartDepth) {
        this.earlyWindowStartDepth = earlyWindowStartDepth;
    }

    public int getEarlyWindowEndDepth() {
        return earlyWindowEndDepth;
    }

    public void setEarlyWindowEndDepth(int earlyWindowEndDepth) {
        this.earlyWindowEndDepth = earlyWindowEndDepth;
    }

    public int getEarlySampleCount() {
        return earlySampleCount;
    }

    public void setEarlySampleCount(int earlySampleCount) {
        this.earlySampleCount = earlySampleCount;
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

    public int getFinalDepth() {
        return finalDepth;
    }

    public void setFinalDepth(int finalDepth) {
        this.finalDepth = finalDepth;
    }

    public int getFinalRank() {
        return finalRank;
    }

    public void setFinalRank(int finalRank) {
        this.finalRank = finalRank;
    }

    public double getFinalRegret() {
        return finalRegret;
    }

    public void setFinalRegret(double finalRegret) {
        this.finalRegret = finalRegret;
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

    public double getFinalStrength() {
        return finalStrength;
    }

    public void setFinalStrength(double finalStrength) {
        this.finalStrength = finalStrength;
    }

    public Double getStrengthGain() {
        return strengthGain;
    }

    public void setStrengthGain(Double strengthGain) {
        this.strengthGain = strengthGain;
    }
}
