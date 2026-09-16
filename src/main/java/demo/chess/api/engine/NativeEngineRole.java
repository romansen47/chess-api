package demo.chess.api.engine;

/**
 * Stable native-engine roles used for runtime selection, availability checks
 * and API capability/error mapping.
 */
public enum NativeEngineRole {
    WHITE_PLAYER("white player"),
    BLACK_PLAYER("black player"),
    EVALUATION("evaluation"),
    DEEP_ANALYSIS("deep analysis");

    private final String displayName;

    NativeEngineRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
