package demo.chess.api.service;

import java.util.ArrayList;
import java.util.List;

import demo.chess.api.dto.AnalysisProfilePointDto;
import demo.chess.api.dto.AnalysisReplayStepDto;
import demo.chess.api.dto.BoardDto;
import demo.chess.game.Game;

/** Converts replay state into stable API response objects. */
final class AnalysisReplayPresenter {

    private final GameService gameService;

    AnalysisReplayPresenter(GameService gameService) {
        this.gameService = gameService;
    }

    AnalysisReplayStepDto inactive(String message) {
        return new AnalysisReplayStepDto(
                false, true, 0, 0, null, null, null,
                0.0, 0.5, 0, null, null, List.of(), message);
    }

    AnalysisReplayStepDto present(
            Game replayGame,
            boolean active,
            boolean done,
            int totalPlies,
            int currentPly,
            String engineName,
            List<AnalysisProfilePointDto> profile,
            String from,
            String to,
            String san,
            double evaluation,
            double bar,
            int depth,
            String message) {
        BoardDto board = gameService.getBoardView(replayGame);
        return new AnalysisReplayStepDto(
                active, done, totalPlies, currentPly,
                from, to, san, Math.round(evaluation * 100.0) / 100.0,
                bar, depth, engineName, board,
                new ArrayList<>(profile), message);
    }

    double latestEvaluation(List<AnalysisProfilePointDto> profile) {
        return profile.isEmpty() ? 0.0
                : profile.get(profile.size() - 1).getEvaluation();
    }

    double latestBar(List<AnalysisProfilePointDto> profile) {
        return profile.isEmpty() ? 0.5
                : profile.get(profile.size() - 1).getBar();
    }

    int latestDepth(List<AnalysisProfilePointDto> profile) {
        return profile.isEmpty() ? 0
                : profile.get(profile.size() - 1).getDepth();
    }
}
