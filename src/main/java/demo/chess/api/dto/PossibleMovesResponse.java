package demo.chess.api.dto;

import java.util.List;

/**
 * Response for a legal-move query from one source square.
 *
 * <p>{@code targets} is the legacy compatibility projection used by older
 * frontend code. {@code moves} is the canonical representation for new code
 * because it carries UCI and Chess960 castling metadata explicitly.</p>
 */
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
