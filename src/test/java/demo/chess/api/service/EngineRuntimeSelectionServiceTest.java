package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import demo.chess.definitions.engines.UciEngineConfig;

/** Direct regression coverage for runtime engine-profile overrides. */
class EngineRuntimeSelectionServiceTest {

    /**
     * Verifies that runtime overrides are normalized, exposed and versioned
     * independently from persistent defaults.
     */
    @Test
    void runtimeOverrideReplacesDefaultAndBumpsOnlyItsSelectionVersion() {
        EngineSettingsService settings = mock(EngineSettingsService.class);
        UciEngineConfig customConfig = mock(UciEngineConfig.class);

        when(settings.getVersion()).thenReturn(7L);
        when(settings.getDefaultWhitePlayerProfileId()).thenReturn("default-white");
        when(settings.getConfig("custom-white")).thenReturn(customConfig);

        EngineRuntimeSelectionService service = new EngineRuntimeSelectionService(settings);
        long before = service.getWhitePlayerVersion();

        service.setWhitePlayerProfileId("  custom-white  ");

        assertEquals("custom-white", service.getEffectiveWhitePlayerProfileId());
        assertEquals("custom-white", service.getAssignments().getWhitePlayerProfileId());
        assertEquals(before + 1, service.getWhitePlayerVersion());
        assertEquals(customConfig, service.requireWhitePlayerConfig());
    }

    /** Verifies that a blank override restores the persistent default profile. */
    @Test
    void blankOverrideRestoresPersistentDefault() {
        EngineSettingsService settings = mock(EngineSettingsService.class);
        when(settings.getDefaultWhitePlayerProfileId()).thenReturn("default-white");
        when(settings.getConfig("custom-white")).thenReturn(mock(UciEngineConfig.class));

        EngineRuntimeSelectionService service = new EngineRuntimeSelectionService(settings);
        service.setWhitePlayerProfileId("custom-white");
        service.setWhitePlayerProfileId(" ");

        assertEquals("default-white", service.getEffectiveWhitePlayerProfileId());
    }
}
