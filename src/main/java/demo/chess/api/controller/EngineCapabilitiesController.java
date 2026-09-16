package demo.chess.api.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.EngineCapabilitiesDto;
import demo.chess.api.dto.EngineCapabilityDto;
import demo.chess.api.engine.NativeEngineAvailability;
import demo.chess.api.engine.NativeEngineRole;
import demo.chess.api.service.EngineAvailabilityService;

/**
 * Exposes the current native-engine capabilities independently from engine
 * configuration management.
 */
@RestController
@RequestMapping("/api/engines")
public class EngineCapabilitiesController {

    private final EngineAvailabilityService engineAvailabilityService;

    public EngineCapabilitiesController(
            EngineAvailabilityService engineAvailabilityService) {
        this.engineAvailabilityService = engineAvailabilityService;
    }

    /**
     * Returns one current native-engine capability snapshot.
     *
     * @return native-engine capabilities
     */
    @GetMapping("/capabilities")
    public EngineCapabilitiesDto getCapabilities() {
        Map<NativeEngineRole, NativeEngineAvailability> availabilities =
                engineAvailabilityService.getAvailabilities();

        return new EngineCapabilitiesDto(
                toDto(require(availabilities, NativeEngineRole.WHITE_PLAYER)),
                toDto(require(availabilities, NativeEngineRole.BLACK_PLAYER)),
                toDto(require(availabilities, NativeEngineRole.EVALUATION)),
                toDto(require(availabilities, NativeEngineRole.DEEP_ANALYSIS)));
    }

    private NativeEngineAvailability require(
            Map<NativeEngineRole, NativeEngineAvailability> availabilities,
            NativeEngineRole role) {
        NativeEngineAvailability availability = availabilities.get(role);
        if (availability == null) {
            throw new IllegalStateException(
                    "Availability snapshot is missing role " + role);
        }
        return availability;
    }

    private EngineCapabilityDto toDto(NativeEngineAvailability availability) {
        return new EngineCapabilityDto(
                availability.configured(),
                availability.available(),
                availability.reason());
    }
}
