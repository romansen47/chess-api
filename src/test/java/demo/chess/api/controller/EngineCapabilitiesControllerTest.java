package demo.chess.api.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import demo.chess.api.engine.NativeEngineAvailability;
import demo.chess.api.engine.NativeEngineAvailabilityReason;
import demo.chess.api.engine.NativeEngineRole;
import demo.chess.api.service.EngineAvailabilityService;

/**
 * Verifies the public engine-capability REST contract.
 */
class EngineCapabilitiesControllerTest {

    @Test
    void exposesStableCapabilitySnapshot() throws Exception {
        EngineAvailabilityService service = mock(EngineAvailabilityService.class);
        when(service.getAvailabilities()).thenReturn(capabilities());

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new EngineCapabilitiesController(service))
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
                .andExpect(jsonPath("$.deepAnalysis.reason", is("UCI_UNRESPONSIVE")));

        verify(service).getAvailabilities();
    }

    private Map<NativeEngineRole, NativeEngineAvailability> capabilities() {
        EnumMap<NativeEngineRole, NativeEngineAvailability> result =
                new EnumMap<>(NativeEngineRole.class);
        result.put(
                NativeEngineRole.WHITE_PLAYER,
                NativeEngineAvailability.available(NativeEngineRole.WHITE_PLAYER));
        result.put(
                NativeEngineRole.BLACK_PLAYER,
                NativeEngineAvailability.notConfigured(NativeEngineRole.BLACK_PLAYER));
        result.put(
                NativeEngineRole.EVALUATION,
                NativeEngineAvailability.unavailable(
                        NativeEngineRole.EVALUATION,
                        NativeEngineAvailabilityReason.EXECUTABLE_NOT_FOUND));
        result.put(
                NativeEngineRole.DEEP_ANALYSIS,
                NativeEngineAvailability.unavailable(
                        NativeEngineRole.DEEP_ANALYSIS,
                        NativeEngineAvailabilityReason.UCI_UNRESPONSIVE));
        return result;
    }
}
