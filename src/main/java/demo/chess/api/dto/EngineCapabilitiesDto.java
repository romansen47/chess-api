package demo.chess.api.dto;

/**
 * Public snapshot of native-engine capabilities.
 *
 * @param whitePlayer White computer-player capability
 * @param blackPlayer Black computer-player capability
 * @param evaluation live-evaluation capability
 * @param deepAnalysis deep-analysis capability
 */
public record EngineCapabilitiesDto(
        EngineCapabilityDto whitePlayer,
        EngineCapabilityDto blackPlayer,
        EngineCapabilityDto evaluation,
        EngineCapabilityDto deepAnalysis) {
}
