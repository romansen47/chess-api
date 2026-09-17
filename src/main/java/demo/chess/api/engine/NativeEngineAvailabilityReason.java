package demo.chess.api.engine;

/**
 * Stable reasons for native-engine availability.
 *
 * <p>{@link #CHESS960_UNSUPPORTED} means the executable is healthy and speaks
 * UCI, but did not advertise the standard {@code UCI_Chess960} capability
 * required by the selected game.</p>
 */
public enum NativeEngineAvailabilityReason {
    AVAILABLE,
    NOT_CONFIGURED,
    EXECUTABLE_NOT_FOUND,
    NOT_EXECUTABLE,
    UCI_UNRESPONSIVE,
    CHESS960_UNSUPPORTED
}
