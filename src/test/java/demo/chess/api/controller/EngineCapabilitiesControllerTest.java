package demo.chess.api.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import demo.chess.api.engine.NativeEngineAvailability;
import demo.chess.api.engine.NativeEngineAvailabilityReason;
import demo.chess.api.engine.NativeEngineRole;
import demo.chess.api.service.EngineAvailabilityService;
import demo.chess.api.service.GameService;
import demo.chess.api.service.UciGameService;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.game.Game;

/** Verifies the public, variant-aware engine-capability REST contract. */
class EngineCapabilitiesControllerTest {

    @Test
    void exposesCapabilitiesForLiveAndSelectedAnalysisVariants() throws Exception {
        EngineAvailabilityService service = mock(EngineAvailabilityService.class);
        GameService gameService = mock(GameService.class);
        UciGameService uciGameService = mock(UciGameService.class);
        Game liveGame = mock(Game.class);

        ChessStartingPosition livePosition = ChessStartingPosition.of(0);
        ChessStartingPosition analysisPosition = ChessStartingPosition.of(959);
        when(gameService.getCurrentGame()).thenReturn(liveGame);
        when(liveGame.getStartingPosition()).thenReturn(livePosition);
        when(uciGameService.getAnalysisStartingPosition()).thenReturn(analysisPosition);

        when(service.getAvailability(NativeEngineRole.WHITE_PLAYER, livePosition))
                .thenReturn(NativeEngineAvailability.available(NativeEngineRole.WHITE_PLAYER));
        when(service.getAvailability(NativeEngineRole.BLACK_PLAYER, livePosition))
                .thenReturn(NativeEngineAvailability.notConfigured(NativeEngineRole.BLACK_PLAYER));
        when(service.getAvailability(NativeEngineRole.EVALUATION, livePosition))
                .thenReturn(NativeEngineAvailability.unavailable(
                        NativeEngineRole.EVALUATION,
                        NativeEngineAvailabilityReason.EXECUTABLE_NOT_FOUND));
        when(service.getAvailability(NativeEngineRole.DEEP_ANALYSIS, analysisPosition))
                .thenReturn(NativeEngineAvailability.unavailable(
                        NativeEngineRole.DEEP_ANALYSIS,
                        NativeEngineAvailabilityReason.CHESS960_UNSUPPORTED));

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new EngineCapabilitiesController(
                        service,
                        gameService,
                        uciGameService))
                .build();

        mvc.perform(get("/api/engines/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whitePlayer.configured", is(true)))
                .andExpect(jsonPath("$.whitePlayer.available", is(true)))
                .andExpect(jsonPath("$.whitePlayer.reason", is("AVAILABLE")))
                .andExpect(jsonPath("$.blackPlayer.configured", is(false)))
                .andExpect(jsonPath("$.blackPlayer.available", is(false)))
                .andExpect(jsonPath("$.blackPlayer.reason", is("NOT_CONFIGURED")))
                .andExpect(jsonPath("$.evaluation.configured", is(true)))
                .andExpect(jsonPath("$.evaluation.available", is(false)))
                .andExpect(jsonPath("$.evaluation.reason", is("EXECUTABLE_NOT_FOUND")))
                .andExpect(jsonPath("$.deepAnalysis.configured", is(true)))
                .andExpect(jsonPath("$.deepAnalysis.available", is(false)))
                .andExpect(jsonPath("$.deepAnalysis.reason", is("CHESS960_UNSUPPORTED")));

        verify(service).getAvailability(NativeEngineRole.WHITE_PLAYER, livePosition);
        verify(service).getAvailability(NativeEngineRole.BLACK_PLAYER, livePosition);
        verify(service).getAvailability(NativeEngineRole.EVALUATION, livePosition);
        verify(service).getAvailability(NativeEngineRole.DEEP_ANALYSIS, analysisPosition);
    }
}
