package demo.chess.api.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import demo.chess.api.dto.AnalysisReplaySettingsDto;
import demo.chess.api.dto.AnalysisReplayStepDto;
import demo.chess.api.service.AnalysisReplayService;
import demo.chess.definitions.engines.management.UciEngineLogEntry;

class AnalysisReplayControllerTest {

    @Test
    void engineLogDelegatesToReplayService() {
        AnalysisReplayService service = mock(AnalysisReplayService.class);
        List<UciEngineLogEntry> expected = List.of(
                new UciEngineLogEntry(1L, "2026-10-30T12:00:00Z",
                        "RESPONSE", "info depth 12"));
        when(service.activeEngineLog()).thenReturn(expected);

        AnalysisReplayController controller = new AnalysisReplayController(service);

        assertSame(expected, controller.engineLog());
        verify(service).activeEngineLog();
    }

    @Test
    void stateDelegatesToReplayService() {
        AnalysisReplayService service = mock(AnalysisReplayService.class);
        AnalysisReplayStepDto expected = new AnalysisReplayStepDto();
        when(service.state()).thenReturn(expected);

        AnalysisReplayController controller = new AnalysisReplayController(service);
        ResponseEntity<?> response = controller.state();

        assertSame(expected, response.getBody());
        verify(service).state();
    }

    @Test
    void startDelegatesToReplayService() throws Exception {
        AnalysisReplayService service = mock(AnalysisReplayService.class);
        AnalysisReplaySettingsDto settings = new AnalysisReplaySettingsDto();
        AnalysisReplayStepDto expected = new AnalysisReplayStepDto();
        when(service.start(settings)).thenReturn(expected);

        AnalysisReplayController controller = new AnalysisReplayController(service);
        ResponseEntity<?> response = controller.start(settings);

        assertSame(expected, response.getBody());
        verify(service).start(settings);
    }

    @Test
    void nextDelegatesToReplayService() throws Exception {
        AnalysisReplayService service = mock(AnalysisReplayService.class);
        AnalysisReplayStepDto expected = new AnalysisReplayStepDto();
        when(service.next()).thenReturn(expected);

        AnalysisReplayController controller = new AnalysisReplayController(service);
        ResponseEntity<?> response = controller.next();

        assertSame(expected, response.getBody());
        verify(service).next();
    }

    @Test
    void cancelDelegatesToReplayService() {
        AnalysisReplayService service = mock(AnalysisReplayService.class);
        AnalysisReplayStepDto expected = new AnalysisReplayStepDto();
        when(service.cancel()).thenReturn(expected);

        AnalysisReplayController controller = new AnalysisReplayController(service);
        ResponseEntity<?> response = controller.cancel();

        assertSame(expected, response.getBody());
        verify(service).cancel();
    }
}
