package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import demo.chess.api.dto.GameSettingsDto;
import demo.chess.definitions.ChessStartingPosition;

class GameServiceChess960Test {

    @Test
    void startsRequestedChess960Position() {
        GameService service = new GameService();
        GameSettingsDto settings = service.getGameSettings();
        settings.setStartingPositionId(0);

        GameSettingsDto applied = service.startNewGame(settings);

        assertEquals(0, applied.getStartingPositionId());
        assertEquals(0, service.getCurrentGame().getStartingPosition().getId());
        assertEquals(
                ChessStartingPosition.of(0).getKingFile(),
                service.getCurrentGame().getWhitePlayer().getKing().getField().getFile());
    }

    @Test
    void keepsClassicalPositionAs518() {
        GameService service = new GameService();
        assertEquals(518, service.getGameSettings().getStartingPositionId());
        assertEquals(518, service.getCurrentGame().getStartingPosition().getId());
    }
}
