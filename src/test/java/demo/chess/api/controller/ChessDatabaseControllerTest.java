package demo.chess.api.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import demo.chess.api.service.ChessDatabaseService;
import demo.chess.api.service.GameLifecycleService;

class ChessDatabaseControllerTest {

    @Test
    void deletesDatabaseGame() throws Exception {
        ChessDatabaseService service = mock(ChessDatabaseService.class);
        MockMvc mvc = controller(service);

        mvc.perform(delete("/api/chess-database/games/42"))
                .andExpect(status().isNoContent());

        verify(service).deleteGame(42L);
    }

    @Test
    void returnsNotFoundWhenDeletingMissingGame() throws Exception {
        ChessDatabaseService service = mock(ChessDatabaseService.class);
        doThrow(new NoSuchElementException()).when(service).deleteGame(404L);

        controller(service).perform(delete("/api/chess-database/games/404"))
                .andExpect(status().isNotFound());
    }

    private MockMvc controller(ChessDatabaseService service) {
        return MockMvcBuilders.standaloneSetup(new ChessDatabaseController(
                service,
                mock(GameLifecycleService.class))).build();
    }
}
