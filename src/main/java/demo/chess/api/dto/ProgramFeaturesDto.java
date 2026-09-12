package demo.chess.api.dto;

/**
 * Runtime feature flags exposed by the backend to the frontend.
 *
 * @param debugMode whether debug-only UI functions may be shown
 */
public record ProgramFeaturesDto(boolean debugMode) {
}
