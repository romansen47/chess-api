package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EngineSettingsVersionsTest {

    @Test
    void assignmentChangesOnlyInvalidateAffectedRuntimeRoles() {
        EngineSettingsVersions versions = new EngineSettingsVersions();

        versions.configurationChanged(true, false, true);

        assertEquals(2L, versions.configuration());
        assertEquals(2L, versions.whitePlayer());
        assertEquals(1L, versions.blackPlayer());
        assertEquals(2L, versions.evaluation());
    }

    @Test
    void profileChangeInvalidatesEveryRoleUsingThatProfile() {
        EngineSettingsVersions versions = new EngineSettingsVersions();

        versions.profileChanged("shared", "shared", "black", "shared");

        assertEquals(2L, versions.configuration());
        assertEquals(2L, versions.whitePlayer());
        assertEquals(1L, versions.blackPlayer());
        assertEquals(2L, versions.evaluation());
    }

    @Test
    void metadataOnlyChangeDoesNotRestartAnyRuntime() {
        EngineSettingsVersions versions = new EngineSettingsVersions();

        versions.configurationChanged();

        assertEquals(2L, versions.configuration());
        assertEquals(1L, versions.whitePlayer());
        assertEquals(1L, versions.blackPlayer());
        assertEquals(1L, versions.evaluation());
    }
}
