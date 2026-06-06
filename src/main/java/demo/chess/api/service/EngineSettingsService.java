package demo.chess.api.service;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.EngineSettingsDto;
import demo.chess.api.dto.UciEngineSettingsDto;
import demo.chess.api.dto.UciEngineSlotSettingsDto;
import demo.chess.definitions.engines.Engine;
import demo.chess.definitions.engines.UciEngineConfig;

@Service
public class EngineSettingsService {

    private static final String WHITE_PLAYER_DISPLAY_NAME = "White player engine";
    private static final String BLACK_PLAYER_DISPLAY_NAME = "Black player engine";
    private static final String EVALUATION_DISPLAY_NAME = "Evaluation engine";

    private final String defaultEnginePath;

    private String whitePlayerEnginePath;
    private String blackPlayerEnginePath;
    private String evaluationEnginePath;

    private UciEngineConfig whitePlayerConfig;
    private UciEngineConfig blackPlayerConfig;
    private UciEngineConfig evaluationConfig;

    private long version;
    private long whitePlayerVersion;
    private long blackPlayerVersion;
    private long evaluationVersion;

    public EngineSettingsService() {
        this.defaultEnginePath = "/usr/games/" + Engine.STOCKFISH_16.path();
        this.whitePlayerEnginePath = defaultEnginePath;
        this.blackPlayerEnginePath = defaultEnginePath;
        this.evaluationEnginePath = defaultEnginePath;
        this.whitePlayerConfig = defaultPlayerConfig();
        this.blackPlayerConfig = defaultPlayerConfig();
        this.evaluationConfig = defaultEvaluationConfig();
        this.version = 1L;
        this.whitePlayerVersion = 1L;
        this.blackPlayerVersion = 1L;
        this.evaluationVersion = 1L;
    }

    public synchronized EngineSettingsDto getSettings() {
        return toDto();
    }

    public synchronized EngineSettingsDto updateSettings(EngineSettingsDto incoming) {
        if (incoming == null) {
            return getSettings();
        }

        boolean changed = false;

        String normalizedWhitePath = normalizeEnginePath(incoming.getWhitePlayer().getEnginePath());
        UciEngineConfig normalizedWhiteConfig = normalizePlayerConfig(
                incoming.getWhitePlayer().getSettings(), whitePlayerConfig);
        if (differentPath(whitePlayerEnginePath, normalizedWhitePath)
                || differentConfig(whitePlayerConfig, normalizedWhiteConfig)) {
            this.whitePlayerEnginePath = normalizedWhitePath;
            this.whitePlayerConfig = normalizedWhiteConfig;
            this.whitePlayerVersion++;
            changed = true;
        }

        String normalizedBlackPath = normalizeEnginePath(incoming.getBlackPlayer().getEnginePath());
        UciEngineConfig normalizedBlackConfig = normalizePlayerConfig(
                incoming.getBlackPlayer().getSettings(), blackPlayerConfig);
        if (differentPath(blackPlayerEnginePath, normalizedBlackPath)
                || differentConfig(blackPlayerConfig, normalizedBlackConfig)) {
            this.blackPlayerEnginePath = normalizedBlackPath;
            this.blackPlayerConfig = normalizedBlackConfig;
            this.blackPlayerVersion++;
            changed = true;
        }

        String normalizedEvaluationPath = normalizeEnginePath(incoming.getEvaluation().getEnginePath());
        UciEngineConfig normalizedEvaluationConfig = normalizeEvaluationConfig(
                incoming.getEvaluation().getSettings(), evaluationConfig);
        if (differentPath(evaluationEnginePath, normalizedEvaluationPath)
                || differentConfig(evaluationConfig, normalizedEvaluationConfig)) {
            this.evaluationEnginePath = normalizedEvaluationPath;
            this.evaluationConfig = normalizedEvaluationConfig;
            this.evaluationVersion++;
            changed = true;
        }

        if (changed) {
            this.version++;
        }

        return getSettings();
    }

    public synchronized UciEngineConfig toPlayerEngineConfig() {
        return copy(whitePlayerConfig);
    }

    public synchronized UciEngineConfig toEvaluationEngineConfig() {
        return copy(evaluationConfig);
    }

    public synchronized long getVersion() {
        return version;
    }

    public synchronized long getWhitePlayerVersion() {
        return whitePlayerVersion;
    }

    public synchronized long getBlackPlayerVersion() {
        return blackPlayerVersion;
    }

    public synchronized long getEvaluationVersion() {
        return evaluationVersion;
    }

    public String getDefaultEnginePath() {
        return defaultEnginePath;
    }

    public synchronized String getEnginePath() {
        return defaultEnginePath;
    }

    public synchronized String getEvaluationEnginePath() {
        return evaluationEnginePath;
    }

    public synchronized void setEvaluationEnginePath(String evaluationEnginePath) {
        this.evaluationEnginePath = normalizeEnginePath(evaluationEnginePath);
    }

    public synchronized String getWhitePlayerEnginePath() {
        return whitePlayerEnginePath;
    }

    public synchronized void setWhitePlayerEnginePath(String whitePlayerEnginePath) {
        this.whitePlayerEnginePath = normalizeEnginePath(whitePlayerEnginePath);
    }

    public synchronized String getBlackPlayerEnginePath() {
        return blackPlayerEnginePath;
    }

    public synchronized void setBlackPlayerEnginePath(String blackPlayerEnginePath) {
        this.blackPlayerEnginePath = normalizeEnginePath(blackPlayerEnginePath);
    }

    public synchronized UciEngineConfig getWhitePlayerConfig() {
        return copy(whitePlayerConfig);
    }

    public synchronized void setWhitePlayerConfig(UciEngineConfig whitePlayerConfig) {
        this.whitePlayerConfig = copy(whitePlayerConfig != null ? whitePlayerConfig : defaultPlayerConfig());
    }

    public synchronized UciEngineConfig getBlackPlayerConfig() {
        return copy(blackPlayerConfig);
    }

    public synchronized void setBlackPlayerConfig(UciEngineConfig blackPlayerConfig) {
        this.blackPlayerConfig = copy(blackPlayerConfig != null ? blackPlayerConfig : defaultPlayerConfig());
    }

    private EngineSettingsDto toDto() {
        return new EngineSettingsDto(
                new UciEngineSlotSettingsDto(
                        WHITE_PLAYER_DISPLAY_NAME,
                        whitePlayerEnginePath,
                        copy(whitePlayerConfig)),
                new UciEngineSlotSettingsDto(
                        BLACK_PLAYER_DISPLAY_NAME,
                        blackPlayerEnginePath,
                        copy(blackPlayerConfig)),
                new UciEngineSlotSettingsDto(
                        EVALUATION_DISPLAY_NAME,
                        evaluationEnginePath,
                        copy(evaluationConfig)),
                version,
                whitePlayerVersion,
                blackPlayerVersion,
                evaluationVersion);
    }

    public UciEngineConfig defaultPlayerConfig() {
        UciEngineConfig result = new UciEngineConfig();

        result.setDepth(0);
        result.setThreads(1);
        result.setHashSize(128);
        result.setMultiPV(0);
        result.setContempt(0);
        result.setMoveOverhead(0);
        result.setUciElo(0);

        return result;
    }

    public UciEngineConfig defaultEvaluationConfig() {
        UciEngineConfig result = new UciEngineConfig();

        result.setDepth(0);
        result.setThreads(2);
        result.setHashSize(256);
        result.setMultiPV(3);
        result.setContempt(0);
        result.setMoveOverhead(0);
        result.setUciElo(0);

        return result;
    }

    private UciEngineConfig normalizePlayerConfig(UciEngineSettingsDto incoming, UciEngineConfig current) {
        UciEngineConfig safeCurrent = current != null ? current : defaultPlayerConfig();
        UciEngineSettingsDto safeIncoming = incoming != null ? incoming : new UciEngineSettingsDto(safeCurrent);

        UciEngineConfig result = new UciEngineConfig();

        result.setDepth(clamp(safeIncoming.getDepth(), 0, 64, safeCurrent.getDepth()));
        result.setThreads(clamp(safeIncoming.getThreads(), 0, 256, safeCurrent.getThreads()));
        result.setHashSize(clamp(safeIncoming.getHashSize(), 1, 262144, safeCurrent.getHashSize()));
        result.setMultiPV(1);
        result.setContempt(clamp(safeIncoming.getContempt(), -1000, 1000, safeCurrent.getContempt()));
        result.setMoveOverhead(clamp(safeIncoming.getMoveOverhead(), 0, 3600, safeCurrent.getMoveOverhead()));
        result.setUciElo(clamp(safeIncoming.getUciElo(), 0, 4000, safeCurrent.getUciElo()));

        return result;
    }

    private UciEngineConfig normalizeEvaluationConfig(UciEngineSettingsDto incoming, UciEngineConfig current) {
        UciEngineConfig safeCurrent = current != null ? current : defaultEvaluationConfig();
        UciEngineSettingsDto safeIncoming = incoming != null ? incoming : new UciEngineSettingsDto(safeCurrent);

        UciEngineConfig result = new UciEngineConfig();

        result.setDepth(clamp(safeIncoming.getDepth(), 0, 64, safeCurrent.getDepth()));
        result.setThreads(clamp(safeIncoming.getThreads(), 1, 256, safeCurrent.getThreads()));
        result.setHashSize(clamp(safeIncoming.getHashSize(), 1, 262144, safeCurrent.getHashSize()));
        result.setMultiPV(clamp(safeIncoming.getMultiPV(), 1, 256, safeCurrent.getMultiPV()));
        result.setContempt(clamp(safeIncoming.getContempt(), -1000, 1000, safeCurrent.getContempt()));
        result.setMoveOverhead(clamp(safeIncoming.getMoveOverhead(), 0, 3600, safeCurrent.getMoveOverhead()));
        result.setUciElo(clamp(safeIncoming.getUciElo(), 0, 4000, safeCurrent.getUciElo()));

        return result;
    }

    private String normalizeEnginePath(String value) {
        if (value == null || value.isBlank()) {
            return defaultEnginePath;
        }
        return value.trim();
    }

    private UciEngineConfig copy(UciEngineConfig source) {
        UciEngineConfig safeSource = source != null ? source : defaultPlayerConfig();
        UciEngineConfig result = new UciEngineConfig();

        result.setDepth(safeSource.getDepth());
        result.setThreads(safeSource.getThreads());
        result.setHashSize(safeSource.getHashSize());
        result.setMultiPV(safeSource.getMultiPV());
        result.setContempt(safeSource.getContempt());
        result.setMoveOverhead(safeSource.getMoveOverhead());
        result.setUciElo(safeSource.getUciElo());

        return result;
    }

    private boolean differentPath(String left, String right) {
        String normalizedLeft = normalizeEnginePath(left);
        String normalizedRight = normalizeEnginePath(right);
        return !normalizedLeft.equals(normalizedRight);
    }

    private boolean differentConfig(UciEngineConfig left, UciEngineConfig right) {
        return left.getDepth() != right.getDepth()
                || left.getThreads() != right.getThreads()
                || left.getHashSize() != right.getHashSize()
                || left.getMultiPV() != right.getMultiPV()
                || left.getContempt() != right.getContempt()
                || left.getMoveOverhead() != right.getMoveOverhead()
                || left.getUciElo() != right.getUciElo();
    }

    private int clamp(int value, int min, int max, int fallback) {
        if (value < min) {
            return fallback;
        }
        return Math.min(value, max);
    }
}
