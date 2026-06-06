package demo.chess.api.dto;

import java.util.List;

public class PossibleMovesResponse {

    private String from;
    private List<String> targets;

    public PossibleMovesResponse(String from, List<String> targets) {
        this.from = from;
        this.targets = targets;
    }

    public String getFrom() {
        return from;
    }

    public List<String> getTargets() {
        return targets;
    }
}
