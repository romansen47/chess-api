package demo.chess.api.dto;

import demo.chess.api.engine.NativeEngineRole;

/**
 * Structured REST error returned when an operation requires a native engine
 * that is not configured for the requested role.
 *
 * @param code stable machine-readable error code
 * @param role missing native-engine role
 * @param message human-readable description
 */
public record EngineUnavailableErrorDto(
        String code,
        NativeEngineRole role,
        String message) {
}
