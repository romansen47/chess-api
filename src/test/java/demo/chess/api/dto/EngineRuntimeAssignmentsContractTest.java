package demo.chess.api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class EngineRuntimeAssignmentsContractTest {

    @Test
    void dtoReadsSharedBackendFrontendContract() throws IOException {
        Path path = Path.of("contracts", "engine-runtime-assignments.json");
        if (!Files.isRegularFile(path)) {
            path = Path.of("..", "contracts", "engine-runtime-assignments.json");
        }
        EngineRuntimeAssignmentsDto dto = new ObjectMapper().readValue(
                Files.readString(path),
                EngineRuntimeAssignmentsDto.class);

        assertEquals("white-fast", dto.getWhitePlayerProfileId());
        assertNull(dto.getBlackPlayerProfileId());
        assertEquals("browser-fallback", dto.getEvaluationProfileId());
    }
}
