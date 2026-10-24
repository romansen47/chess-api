package demo.chess.api.dto;

import java.util.ArrayList;
import java.util.List;

/** Persistent JSON store for the reusable opening library. */
public class OpeningStoreDto {

    private long version = 1L;
    private List<Entry> openings = new ArrayList<>();

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public List<Entry> getOpenings() { return openings; }
    public void setOpenings(List<Entry> openings) {
        this.openings = openings == null ? new ArrayList<>() : new ArrayList<>(openings);
    }

    /** Persisted source-of-truth fields for one opening. */
    public static class Entry {
        private String id;
        private String name;
        private String eco;
        private int startingPositionId;
        private List<String> moves = new ArrayList<>();

        public Entry() {
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEco() { return eco; }
        public void setEco(String eco) { this.eco = eco; }
        public int getStartingPositionId() { return startingPositionId; }
        public void setStartingPositionId(int startingPositionId) { this.startingPositionId = startingPositionId; }
        public List<String> getMoves() { return moves; }
        public void setMoves(List<String> moves) {
            this.moves = moves == null ? new ArrayList<>() : new ArrayList<>(moves);
        }
    }
}
