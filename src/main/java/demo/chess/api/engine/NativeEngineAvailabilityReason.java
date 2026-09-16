package demo.chess.api.engine;

/**
 * Stable reasons for native-engine availability.
 */
public enum NativeEngineAvailabilityReason {
    AVAILABLE,
    NOT_CONFIGURED,
    EXECUTABLE_NOT_FOUND,
    NOT_EXECUTABLE,
    UCI_UNRESPONSIVE
}
