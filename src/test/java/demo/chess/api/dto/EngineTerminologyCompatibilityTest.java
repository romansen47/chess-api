package demo.chess.api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class EngineTerminologyCompatibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsLegacyGameAnalysisAssignmentButWritesCurrentName() throws Exception {
        EngineProfileAssignmentsDto assignments = objectMapper.readValue(
                "{\"deepAnalysisProfileId\":\"legacy-profile\"}",
                EngineProfileAssignmentsDto.class);

        assertEquals("legacy-profile", assignments.getGameAnalysisProfileId());
        String serialized = objectMapper.writeValueAsString(assignments);
        assertTrue(serialized.contains("\"gameAnalysisProfileId\":\"legacy-profile\""));
        assertFalse(serialized.contains("deepAnalysisProfileId"));
    }

    @Test
    void readsLegacyTypedConfigurationAssignment() throws Exception {
        EngineConfigStoreDto store = objectMapper.readValue(
                "{\"defaultDeepAnalysisConfigId\":\"legacy-config\"}",
                EngineConfigStoreDto.class);

        assertEquals("legacy-config", store.getLegacyDefaultGameAnalysisConfigId());
    }
}
