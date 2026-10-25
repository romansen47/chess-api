package demo.chess.api.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import demo.chess.definitions.ChessStartingPosition;

class UciGameDtoTest {

    @Test
    void exposesSemanticStandardStartingPositionWithoutInspectingFenInClients() {
        UciGameDto standard = new UciGameDto(
                0, "white", "", List.of(), "White", "Black",
                null, List.of(), ChessStartingPosition.STANDARD_ID,
                ChessStartingPosition.STANDARD.initialFen());
        UciGameDto chess960 = new UciGameDto(
                0, "white", "", List.of(), "White", "Black",
                null, List.of(), 0, ChessStartingPosition.of(0).initialFen());

        assertTrue(standard.isStandardStartingPosition());
        assertFalse(chess960.isStandardStartingPosition());
    }
}
