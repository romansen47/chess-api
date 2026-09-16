package demo.chess.api.exception;

import java.util.Objects;

import demo.chess.api.engine.NativeEngineRole;

/**
 * Signals that an application operation requires a native UCI engine but no
 * engine profile is configured for the corresponding role.
 *
 * <p>This is an application-level state, not a transport-level error. REST
 * mapping is deliberately handled separately by the API layer.</p>
 */
public class NativeEngineUnavailableException extends IllegalStateException {

    private final NativeEngineRole role;

    /**
     * Creates a new exception for the missing native-engine role.
     * @param role the role that requires a native engine
     */
    public NativeEngineUnavailableException(NativeEngineRole role) {
        super("No native engine is configured for " + Objects.requireNonNull(role, "role").getDisplayName());
        this.role = role;
    }

    /**
     * Returns the missing native-engine role.
     * @return the role
     */
    public NativeEngineRole getRole() {
        return role;
    }

}
