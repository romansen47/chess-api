package demo.chess.api.dto;

import java.util.List;

public class PossibleMovesResponse {

    private String from;
    private List<String> targets;

    /**
     * Creates a new PossibleMovesResponse instance.
     * @param from the from
     * @param targets the targets
     */
    public PossibleMovesResponse(String from, List<String> targets) {
        this.from = from;
        this.targets = targets;
    }

    /**
     * Returns the from.
     * @return the from
     */
    public String getFrom() {
        return from;
    }

    /**
     * Returns the targets.
     * @return the targets
     */
    public List<String> getTargets() {
        return targets;
    }
}
