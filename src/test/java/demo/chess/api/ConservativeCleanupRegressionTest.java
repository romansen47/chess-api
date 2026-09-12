package demo.chess.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import tools.jackson.databind.ObjectMapper;

import demo.chess.api.controller.ComputerMoveController;
import demo.chess.api.dto.EngineConfigStoreDto;
import demo.chess.api.dto.ManagedEngineConfigDto;
import demo.chess.api.dto.MoveResultDto;
import demo.chess.api.service.ComputerMoveService;

class ConservativeCleanupRegressionTest {

    @Test
    void computerMoveControllerDelegatesToCurrentServiceEntryPoint() throws Exception {
        ComputerMoveService service = mock(ComputerMoveService.class);
        MoveResultDto expected = new MoveResultDto(
                true,
                null,
                "e2",
                "e4",
                "e4",
                "black",
                "position",
                null);
        when(service.makeComputerMove()).thenReturn(expected);

        ComputerMoveController controller = new ComputerMoveController(service);
        ResponseEntity<MoveResultDto> response = controller.makeComputerMove();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(service).makeComputerMove();
    }

    @Test
    void legacyCombinedEngineStoreStillDeserializesForMigration() throws Exception {
        String json = """
                {
                  "configs": [
                    {
                      "id": "legacy-profile",
                      "name": "Legacy profile",
                      "type": "PLAYER",
                      "engine": "/opt/chess/legacy-engine",
                      "engineName": "Legacy Engine",
                      "engineAuthor": "Test",
                      "depth": 12,
                      "moveTimeSeconds": 3,
                      "options": {
                        "Hash": {
                          "type": "spin",
                          "defaultValue": "16",
                          "value": "32",
                          "min": 1,
                          "max": 1024,
                          "vars": []
                        }
                      }
                    }
                  ]
                }
                """;

        EngineConfigStoreDto store = new ObjectMapper().readValue(json, EngineConfigStoreDto.class);

        assertEquals(1, store.getLegacyConfigs().size());
        ManagedEngineConfigDto legacy = store.getLegacyConfigs().get(0);
        assertEquals("legacy-profile", legacy.getId());
        assertEquals("Legacy Engine", legacy.getEngineName());
        assertEquals("/opt/chess/legacy-engine", legacy.getEngine());
        assertEquals("32", legacy.getOptions().get("Hash").getValue());
    }
}
