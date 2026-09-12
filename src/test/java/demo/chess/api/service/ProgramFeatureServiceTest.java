package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProgramFeatureServiceTest {

    private final ProgramFeatureService service =
            new ProgramFeatureService();

    @AfterEach
    void clearDebugModeProperty() {
        System.clearProperty(
                ProgramFeatureService.DEBUG_MODE_PROPERTY);
    }

    @Test
    void debugModeIsDisabledWhenPropertyIsMissing() {
        System.clearProperty(
                ProgramFeatureService.DEBUG_MODE_PROPERTY);

        assertFalse(service.isDebugModeEnabled());
    }

    @Test
    void debugModeIsEnabledByPropertyPresence() {
        System.setProperty(
                ProgramFeatureService.DEBUG_MODE_PROPERTY,
                "");

        assertTrue(service.isDebugModeEnabled());
    }

    @Test
    void debugModeIsEnabledByTrueValue() {
        System.setProperty(
                ProgramFeatureService.DEBUG_MODE_PROPERTY,
                "true");

        assertTrue(service.isDebugModeEnabled());
    }

    @Test
    void debugModeCanBeExplicitlyDisabled() {
        System.setProperty(
                ProgramFeatureService.DEBUG_MODE_PROPERTY,
                "false");

        assertFalse(service.isDebugModeEnabled());
    }
}
