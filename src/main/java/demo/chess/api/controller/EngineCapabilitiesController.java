package demo.chess.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.EngineCapabilitiesDto;
import demo.chess.api.dto.EngineCapabilityDto;
import demo.chess.api.engine.NativeEngineAvailability;
import demo.chess.api.engine.NativeEngineRole;
import demo.chess.api.service.EngineAvailabilityService;
import demo.chess.api.service.GameService;
import demo.chess.api.service.UciGameService;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.game.Game;

/**
 * Exposes native-engine capabilities for the variants that are currently in
 * use by the application.
 *
 * <p>Player and live-evaluation roles are checked against the live game. Deep
 * analysis is checked against the currently selected analysis game, which may
 * be an imported Chess960 PGN with a different starting position.</p>
 */
@RestController
@RequestMapping("/api/engines")
public class EngineCapabilitiesController {

    private final EngineAvailabilityService engineAvailabilityService;
    private final GameService gameService;
    private final UciGameService uciGameService;

    public EngineCapabilitiesController(
            EngineAvailabilityService engineAvailabilityService,
            GameService gameService,
            UciGameService uciGameService) {
        this.engineAvailabilityService = engineAvailabilityService;
        this.gameService = gameService;
        this.uciGameService = uciGameService;
    }

    /**
     * Returns one current, variant-aware native-engine capability snapshot.
     *
     * @return native-engine capabilities
     */
    @GetMapping("/capabilities")
    public EngineCapabilitiesDto getCapabilities() {
        ChessStartingPosition livePosition = liveStartingPosition();
        ChessStartingPosition analysisPosition = uciGameService.getAnalysisStartingPosition();

        return new EngineCapabilitiesDto(
                toDto(engineAvailabilityService.getAvailability(
                        NativeEngineRole.WHITE_PLAYER, livePosition)),
                toDto(engineAvailabilityService.getAvailability(
                        NativeEngineRole.BLACK_PLAYER, livePosition)),
                toDto(engineAvailabilityService.getAvailability(
                        NativeEngineRole.EVALUATION, livePosition)),
                toDto(engineAvailabilityService.getAvailability(
                        NativeEngineRole.DEEP_ANALYSIS, analysisPosition)));
    }

    private ChessStartingPosition liveStartingPosition() {
        Game game = gameService.getCurrentGame();
        return game != null && game.getStartingPosition() != null
                ? game.getStartingPosition()
                : ChessStartingPosition.STANDARD;
    }

    private EngineCapabilityDto toDto(NativeEngineAvailability availability) {
        return new EngineCapabilityDto(
                availability.configured(),
                availability.available(),
                availability.reason());
    }
}
