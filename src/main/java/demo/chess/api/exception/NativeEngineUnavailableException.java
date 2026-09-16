package demo.chess.api.exception;

import java.util.Objects;

/**
 * Signals that an application operation requires a native UCI engine but no
 * engine profile is configured for the corresponding role.
 *
 * <p>This is an application-level state, not a transport-level error. REST
 * mapping is deliberately handled separately by the API layer.</p>
 */
public class NativeEngineUnavailableException extends IllegalStateException {

    private final Role role;

    /**
     * Creates a new exception for the missing native-engine role.
     * @param role the role that requires a native engine
     */
    public NativeEngineUnavailableException(Role role) {
        super("No native engine is configured for " + Objects.requireNonNull(role, "role").getDisplayName());
        this.role = role;
    }

    /**
     * Returns the missing native-engine role.
     * @return the role
     */
    public Role getRole() {
        return role;
    }

    /**
     * Stable native-engine roles used by application services and, later, API
     * capability/error mapping.
     */
    public enum Role {
        WHITE_PLAYER("white player"),
        BLACK_PLAYER("black player"),
        EVALUATION("evaluation"),
        DEEP_ANALYSIS("deep analysis");

        private final String displayName;

        Role(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
