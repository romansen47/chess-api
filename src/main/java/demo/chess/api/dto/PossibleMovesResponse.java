package demo.chess.api.dto;

import java.util.List;

public class PossibleMovesResponse {

    private final String from;
    private final List<String> targets;
    private final List<LegalMoveDto> moves;

    public PossibleMovesResponse(String from, List<String> targets) {
        this(from, targets, List.of());
    }

    public PossibleMovesResponse(String from, List<String> targets, List<LegalMoveDto> moves) {
        this.from = from;
        this.targets = targets != null ? List.copyOf(targets) : List.of();
        this.moves = moves != null ? List.copyOf(moves) : List.of();
    }

    public String getFrom() {
        return from;
    }

    public List<String> getTargets() {
        return targets;
    }

    public List<LegalMoveDto> getMoves() {
        return moves;
    }
}
