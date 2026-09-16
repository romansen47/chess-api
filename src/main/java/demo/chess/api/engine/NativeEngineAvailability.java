package demo.chess.api.engine;

import java.util.Objects;

/**
 * Current usability state of one native-engine role.
 *
 * @param role native-engine role
 * @param configured whether a concrete engine profile is assigned
 * @param available whether the assigned engine currently completes a UCI handshake
 * @param reason stable reason for the current state
 */
public record NativeEngineAvailability(
        NativeEngineRole role,
        boolean configured,
        boolean available,
        NativeEngineAvailabilityReason reason) {

    public NativeEngineAvailability {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(reason, "reason");

        if (!configured && reason != NativeEngineAvailabilityReason.NOT_CONFIGURED) {
            throw new IllegalArgumentException(
                    "An unconfigured engine role must use NOT_CONFIGURED");
        }
        if (available && (!configured || reason != NativeEngineAvailabilityReason.AVAILABLE)) {
            throw new IllegalArgumentException(
                    "An available engine role must be configured and use AVAILABLE");
        }
        if (reason == NativeEngineAvailabilityReason.AVAILABLE && !available) {
            throw new IllegalArgumentException(
                    "AVAILABLE requires available=true");
        }
        if (configured && reason == NativeEngineAvailabilityReason.NOT_CONFIGURED) {
            throw new IllegalArgumentException(
                    "A configured engine role cannot use NOT_CONFIGURED");
        }
    }

    public static NativeEngineAvailability notConfigured(NativeEngineRole role) {
        return new NativeEngineAvailability(
                role,
                false,
                false,
                NativeEngineAvailabilityReason.NOT_CONFIGURED);
    }

    public static NativeEngineAvailability unavailable(
            NativeEngineRole role,
            NativeEngineAvailabilityReason reason) {
        return new NativeEngineAvailability(role, true, false, reason);
    }

    public static NativeEngineAvailability available(NativeEngineRole role) {
        return new NativeEngineAvailability(
                role,
                true,
                true,
                NativeEngineAvailabilityReason.AVAILABLE);
    }
}
