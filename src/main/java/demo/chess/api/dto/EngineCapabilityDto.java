package demo.chess.api.dto;

import demo.chess.api.engine.NativeEngineAvailabilityReason;

/**
 * Public capability state for one native-engine role.
 *
 * @param configured whether an engine profile is assigned
 * @param available whether the assigned engine is currently usable
 * @param reason stable availability reason
 */
public record EngineCapabilityDto(
        boolean configured,
        boolean available,
        NativeEngineAvailabilityReason reason) {
}
