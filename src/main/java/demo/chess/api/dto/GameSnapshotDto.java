package demo.chess.api.dto;

public class GameSnapshotDto {

    private boolean importedAnalysisGame;
    private UciGameDto game;

    /**
     * Creates a new GameSnapshotDto instance.
     */
    public GameSnapshotDto() {
    }

    /**
     * Creates a new GameSnapshotDto instance.
     * @param importedAnalysisGame whether the current frontend game is an imported analysis game
     * @param game the current game snapshot
     */
    public GameSnapshotDto(boolean importedAnalysisGame, UciGameDto game) {
        this.importedAnalysisGame = importedAnalysisGame;
        this.game = game;
    }

    public boolean isImportedAnalysisGame() {
        return importedAnalysisGame;
    }

    public void setImportedAnalysisGame(boolean importedAnalysisGame) {
        this.importedAnalysisGame = importedAnalysisGame;
    }

    public UciGameDto getGame() {
        return game;
    }

    public void setGame(UciGameDto game) {
        this.game = game;
    }
}
