package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import demo.chess.api.dto.GameSettingsDto;
import demo.chess.definitions.ChessStartingPosition;

class GameSettingsPolicyTest {

    @Test
    void defaultsToClassicalPosition518() {
        GameSettingsDto defaults = GameSettingsPolicy.defaults();
        assertEquals(ChessStartingPosition.STANDARD_ID, defaults.getStartingPositionId());
        assertEquals(GameService.DEFAULT_TIME_SECONDS, defaults.getTimeForEachPlayerSeconds());
    }

    @Test
    void normalizesMutableRequestBeforeCreatingGame() {
        GameSettingsDto current = GameSettingsPolicy.defaults();
        current.setVersion(7);
        GameSettingsDto request = new GameSettingsDto();
        request.setTimeForEachPlayerSeconds(0);
        request.setIncrementForWhiteSeconds(-3);
        request.setIncrementForBlackSeconds(2);
        request.setAdditionalTimeAfter40MovesSeconds(-10);
        request.setStartingColor(" black ");
        request.setStartingPositionId(0);

        GameSettingsDto normalized = GameSettingsPolicy.normalize(request, current);

        assertEquals(GameService.DEFAULT_TIME_SECONDS, normalized.getTimeForEachPlayerSeconds());
        assertEquals(0, normalized.getIncrementForWhiteSeconds());
        assertEquals(2, normalized.getIncrementForBlackSeconds());
        assertEquals(0, normalized.getAdditionalTimeAfter40MovesSeconds());
        assertEquals("BLACK", normalized.getStartingColor());
        assertEquals(0, normalized.getStartingPositionId());
        assertEquals(8, normalized.getVersion());
    }

    @Test
    void rejectsOutOfRangeChess960PositionAtPolicyBoundary() {
        GameSettingsDto request = GameSettingsPolicy.defaults();
        request.setStartingPositionId(960);
        assertThrows(IllegalArgumentException.class,
                () -> GameSettingsPolicy.normalize(request, GameSettingsPolicy.defaults()));
    }
}
