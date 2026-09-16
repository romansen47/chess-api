package demo.chess.api.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import demo.chess.api.engine.NativeEngineRole;
import demo.chess.api.exception.NativeEngineUnavailableException;
import demo.chess.api.service.AnalysisEvaluationService;
import demo.chess.api.service.AnalysisReplayService;
import demo.chess.api.service.ComputerMoveService;
import demo.chess.api.service.EvaluationService;
import demo.chess.api.service.LiveEvaluationStreamService;

/**
 * Verifies the central REST contract for missing native engines and ensures
 * controller-local generic error handling does not swallow that contract.
 */
class ApiExceptionHandlerTest {

    @Test
    void evaluationEndpointReturnsStructured503() throws Exception {
        EvaluationService evaluationService = mock(EvaluationService.class);
        when(evaluationService.getEvaluation()).thenThrow(
                new NativeEngineUnavailableException(NativeEngineRole.EVALUATION));

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new EngineController(
                        evaluationService,
                        mock(LiveEvaluationStreamService.class)))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(get("/api/eval"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code", is("ENGINE_UNAVAILABLE")))
                .andExpect(jsonPath("$.role", is("EVALUATION")))
                .andExpect(jsonPath(
                        "$.message",
                        is("No native engine is configured for evaluation")));
    }

    @Test
    void configuredButUnstartableEngineUsesSameStructured503Contract() throws Exception {
        EvaluationService evaluationService = mock(EvaluationService.class);
        when(evaluationService.getEvaluation()).thenThrow(
                NativeEngineUnavailableException.startFailure(
                        NativeEngineRole.EVALUATION,
                        new IllegalStateException("UCI handshake failed")));

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new EngineController(
                        evaluationService,
                        mock(LiveEvaluationStreamService.class)))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(get("/api/eval"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code", is("ENGINE_UNAVAILABLE")))
                .andExpect(jsonPath("$.role", is("EVALUATION")))
                .andExpect(jsonPath(
                        "$.message",
                        is("Native engine for evaluation is configured but could not be started")));
    }

    @Test
    void computerMoveEndpointReturnsRoleSpecificStructured503() throws Exception {
        ComputerMoveService computerMoveService = mock(ComputerMoveService.class);
        when(computerMoveService.makeComputerMove()).thenThrow(
                new NativeEngineUnavailableException(NativeEngineRole.WHITE_PLAYER));

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new ComputerMoveController(computerMoveService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(post("/api/computer-move"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code", is("ENGINE_UNAVAILABLE")))
                .andExpect(jsonPath("$.role", is("WHITE_PLAYER")))
                .andExpect(jsonPath(
                        "$.message",
                        is("No native engine is configured for white player")));
    }

    @Test
    void analysisEvaluationEndpointReturnsStructured503() throws Exception {
        AnalysisEvaluationService analysisEvaluationService =
                mock(AnalysisEvaluationService.class);
        when(analysisEvaluationService.getEvaluation(12)).thenThrow(
                new NativeEngineUnavailableException(NativeEngineRole.EVALUATION));

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new AnalysisEvaluationController(analysisEvaluationService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(get("/api/analysis-eval").param("ply", "12"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code", is("ENGINE_UNAVAILABLE")))
                .andExpect(jsonPath("$.role", is("EVALUATION")))
                .andExpect(jsonPath(
                        "$.message",
                        is("No native engine is configured for evaluation")));
    }

    @Test
    void deepAnalysisStartEndpointUsesSameCentralContract() throws Exception {
        AnalysisReplayService replayService = mock(AnalysisReplayService.class);
        when(replayService.start(null)).thenThrow(
                new NativeEngineUnavailableException(NativeEngineRole.DEEP_ANALYSIS));

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new AnalysisReplayController(replayService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(post("/api/analysis-replay/start"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code", is("ENGINE_UNAVAILABLE")))
                .andExpect(jsonPath("$.role", is("DEEP_ANALYSIS")))
                .andExpect(jsonPath(
                        "$.message",
                        is("No native engine is configured for deep analysis")));
    }
}
