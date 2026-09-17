package demo.chess.api.service;

import java.util.Locale;

import demo.chess.api.dto.GameSettingsDto;
import demo.chess.definitions.ChessStartingPosition;

/**
 * Normalization policy for new-game API settings.
 *
 * <p>Transport DTOs remain deliberately simple mutable Jackson objects. This
 * policy is the boundary that turns such input into validated application
 * settings before a core game is created.</p>
 */
final class GameSettingsPolicy {

    private GameSettingsPolicy() {
    }

    static GameSettingsDto defaults() {
        return new GameSettingsDto(
                GameService.DEFAULT_TIME_SECONDS,
                GameService.DEFAULT_INCREMENT_SECONDS,
                GameService.DEFAULT_INCREMENT_SECONDS,
                0,
                "WHITE",
                ChessStartingPosition.STANDARD_ID,
                0);
    }

    static GameSettingsDto normalize(GameSettingsDto requested, GameSettingsDto current) {
        GameSettingsDto source = requested != null ? requested : current;
        if (source == null) source = defaults();

        int timeForEachPlayerSeconds = source.getTimeForEachPlayerSeconds() > 0
                ? source.getTimeForEachPlayerSeconds()
                : GameService.DEFAULT_TIME_SECONDS;
        int incrementForWhiteSeconds = Math.max(0, source.getIncrementForWhiteSeconds());
        int incrementForBlackSeconds = Math.max(0, source.getIncrementForBlackSeconds());
        int additionalTimeAfter40MovesSeconds = Math.max(0, source.getAdditionalTimeAfter40MovesSeconds());
        String startingColor = source.getStartingColor() != null && !source.getStartingColor().isBlank()
                ? source.getStartingColor().trim().toUpperCase(Locale.ROOT)
                : "WHITE";

        ChessStartingPosition startingPosition = ChessStartingPosition.of(source.getStartingPositionId());
        long nextVersion = current != null ? current.getVersion() + 1 : 1;
        return new GameSettingsDto(
                timeForEachPlayerSeconds,
                incrementForWhiteSeconds,
                incrementForBlackSeconds,
                additionalTimeAfter40MovesSeconds,
                startingColor,
                startingPosition.getId(),
                nextVersion);
    }

    static GameSettingsDto copy(GameSettingsDto settings) {
        if (settings == null) return defaults();
        return new GameSettingsDto(
                settings.getTimeForEachPlayerSeconds(),
                settings.getIncrementForWhiteSeconds(),
                settings.getIncrementForBlackSeconds(),
                settings.getAdditionalTimeAfter40MovesSeconds(),
                settings.getStartingColor(),
                settings.getStartingPositionId(),
                settings.getVersion());
    }
}
