package demo.chess.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import demo.chess.definitions.engines.UciEngineConfig;

/**
 * REST-/UI-Sicht auf eine UCI-Engine-Konfiguration.
 *
 * Fachlich trägt diese Klasse eine UciEngineConfig und delegiert die einzelnen
 * Konfigurationswerte nur an diese Instanz. Im JSON erscheint die Konfiguration
 * trotzdem flach, z.B. { "depth": 18, "threads": 1, ... }.
 */
public class UciEngineSettingsDto {

    @JsonIgnore
    private UciEngineConfig config;

    public UciEngineSettingsDto() {
        this.config = new UciEngineConfig();
    }

    public UciEngineSettingsDto(UciEngineConfig config) {
        this.config = config != null ? config : new UciEngineConfig();
    }

    @JsonIgnore
    public UciEngineConfig getConfig() {
        return config;
    }

    public void setConfig(UciEngineConfig config) {
        this.config = config != null ? config : new UciEngineConfig();
    }

    public int getDepth() {
        return config.getDepth();
    }

    public void setDepth(int depth) {
        config.setDepth(depth);
    }

    public int getThreads() {
        return config.getThreads();
    }

    public void setThreads(Integer threads) {
        config.setThreads(threads);
    }

    public int getHashSize() {
        return config.getHashSize();
    }

    public void setHashSize(Integer hashSize) {
        config.setHashSize(hashSize);
    }

    public int getMultiPV() {
        return config.getMultiPV();
    }

    public void setMultiPV(Integer multiPV) {
        config.setMultiPV(multiPV);
    }

    public int getContempt() {
        return config.getContempt();
    }

    public void setContempt(Integer contempt) {
        config.setContempt(contempt);
    }

    public int getMoveOverhead() {
        return config.getMoveOverhead();
    }

    public void setMoveOverhead(Integer moveOverhead) {
        config.setMoveOverhead(moveOverhead);
    }

    public int getUciElo() {
        return config.getUciElo();
    }

    public void setUciElo(Integer uciElo) {
        config.setUciElo(uciElo);
    }
}
