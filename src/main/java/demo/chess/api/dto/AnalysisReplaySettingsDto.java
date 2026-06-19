package demo.chess.api.dto;

public class AnalysisReplaySettingsDto {

    private String enginePath;
    private int moveTimeSeconds;
    private int depth;
    private int threads;
    private int hashSize;
    private int multiPV;
    private int contempt;
    private int uciElo;

    public AnalysisReplaySettingsDto() {
    }

    public AnalysisReplaySettingsDto(
            String enginePath,
            int moveTimeSeconds,
            int depth,
            int threads,
            int hashSize,
            int multiPV,
            int contempt,
            int uciElo) {
        this.enginePath = enginePath;
        this.moveTimeSeconds = moveTimeSeconds;
        this.depth = depth;
        this.threads = threads;
        this.hashSize = hashSize;
        this.multiPV = multiPV;
        this.contempt = contempt;
        this.uciElo = uciElo;
    }

    public String getEnginePath() {
        return enginePath;
    }

    public void setEnginePath(String enginePath) {
        this.enginePath = enginePath;
    }

    public int getMoveTimeSeconds() {
        return moveTimeSeconds;
    }

    public void setMoveTimeSeconds(int moveTimeSeconds) {
        this.moveTimeSeconds = moveTimeSeconds;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getHashSize() {
        return hashSize;
    }

    public void setHashSize(int hashSize) {
        this.hashSize = hashSize;
    }

    public int getMultiPV() {
        return multiPV;
    }

    public void setMultiPV(int multiPV) {
        this.multiPV = multiPV;
    }

    public int getContempt() {
        return contempt;
    }

    public void setContempt(int contempt) {
        this.contempt = contempt;
    }

    public int getUciElo() {
        return uciElo;
    }

    public void setUciElo(int uciElo) {
        this.uciElo = uciElo;
    }
}
