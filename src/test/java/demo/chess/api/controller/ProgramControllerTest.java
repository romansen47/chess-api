package demo.chess.api.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import demo.chess.api.dto.ProgramFeaturesDto;
import demo.chess.api.service.ProgramFeatureService;

class ProgramControllerTest {

    @Test
    void exposesDisabledDebugMode() {
        ProgramFeatureService featureService =
                mock(ProgramFeatureService.class);
        when(featureService.isDebugModeEnabled())
                .thenReturn(false);

        ProgramController controller = new ProgramController(
                mock(ConfigurableApplicationContext.class),
                featureService);

        ProgramFeaturesDto features =
                controller.getProgramFeatures();

        assertFalse(features.debugMode());
    }

    @Test
    void exposesEnabledDebugMode() {
        ProgramFeatureService featureService =
                mock(ProgramFeatureService.class);
        when(featureService.isDebugModeEnabled())
                .thenReturn(true);

        ProgramController controller = new ProgramController(
                mock(ConfigurableApplicationContext.class),
                featureService);

        ProgramFeaturesDto features =
                controller.getProgramFeatures();

        assertTrue(features.debugMode());
    }
}
