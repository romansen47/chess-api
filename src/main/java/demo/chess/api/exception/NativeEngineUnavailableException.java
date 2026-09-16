package demo.chess.api.exception;

import java.util.Objects;

import demo.chess.api.engine.NativeEngineRole;

/**
 * Signals that an application operation requires a native UCI engine that is
 * currently unavailable for the corresponding role.
 *
 * <p>This includes both an unconfigured role and a configured engine that
 * cannot be started. It is an application-level state, not a transport-level
 * error. REST mapping is deliberately handled separately by the API layer.</p>
 */
public class NativeEngineUnavailableException extends IllegalStateException {

    private final NativeEngineRole role;

    /**
     * Creates a new exception for the missing native-engine role.
     * @param role the role that requires a native engine
     */
    public NativeEngineUnavailableException(NativeEngineRole role) {
        this(
                role,
                "No native engine is configured for "
                        + Objects.requireNonNull(role, "role").getDisplayName(),
                null);
    }

    private NativeEngineUnavailableException(
            NativeEngineRole role,
            String message,
            Throwable cause) {
        super(message, cause);
        this.role = Objects.requireNonNull(role, "role");
    }

    /**
     * Creates an unavailable-engine error for a configured engine that failed
     * during process startup or UCI initialization.
     *
     * @param role the role that requires the engine
     * @param cause native engine startup failure
     * @return role-specific unavailable-engine error
     */
    public static NativeEngineUnavailableException startFailure(
            NativeEngineRole role,
            Throwable cause) {
        NativeEngineRole requiredRole = Objects.requireNonNull(role, "role");
        return new NativeEngineUnavailableException(
                requiredRole,
                "Native engine for " + requiredRole.getDisplayName()
                        + " is configured but could not be started",
                Objects.requireNonNull(cause, "cause"));
    }

    /**
     * Returns the missing native-engine role.
     * @return the role
     */
    public NativeEngineRole getRole() {
        return role;
    }

}
