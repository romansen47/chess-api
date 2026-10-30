package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import demo.chess.api.dto.EngineLineDto;
import demo.chess.definitions.engines.DeepAnalysisEngine;
import demo.chess.definitions.engines.DeepAnalysisResult;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.UciEngineConfig;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.game.Game;
import demo.chess.game.TerminalPositionEvaluator;
import demo.chess.game.impl.Simulation;

/** Performs one engine evaluation without owning replay-session lifecycle. */
final class AnalysisStepAnalyzer {

    private final EngineLineDisplayService engineLineDisplayService;

    AnalysisStepAnalyzer(EngineLineDisplayService engineLineDisplayService) {
        this.engineLineDisplayService = engineLineDisplayService;
    }

    AnalysisStepEvaluation analyze(
            Game replayGame,
            DeepAnalysisEngine engine,
            UciEngineConfig engineConfig,
            int fallbackDepth) throws NoMoveFoundException {
        Double terminal = TerminalEvaluationMapper.toEvaluation(
                TerminalPositionEvaluator.determineState(replayGame));
        if (terminal != null) {
            return new AnalysisStepEvaluation(
                    terminal,
                    EvaluationBarMapper.toBar(terminal),
                    fallbackDepth,
                    List.of(),
                    null,
                    false);
        }

        try {
            engine.clearCachedLines();
            DeepAnalysisResult result = engine.analyze(replayGame, engineConfig);
            List<EngineLine> bestLines = result.getFinalLines();
            if (bestLines.isEmpty()) {
                return new AnalysisStepEvaluation(0.0, 0.5, 0, List.of(), result, false);
            }

            EngineLine bestLine = bestLines.get(0);
            List<EngineLineDto> lines = new ArrayList<>();
            for (EngineLine line : bestLines) {
                Game displayGame = Simulation.forkDummyFrom(replayGame.getMoveList());
                lines.add(engineLineDisplayService.toDto(displayGame, line));
            }
            return new AnalysisStepEvaluation(
                    bestLine.getEvaluation(),
                    EvaluationBarMapper.toBar(bestLine.getEvaluation()),
                    bestLine.getDepth(),
                    lines,
                    result,
                    false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return AnalysisStepEvaluation.cancelledResult();
        } catch (ExecutionException | IOException e) {
            return AnalysisStepEvaluation.cancelledResult();
        }
    }

    record AnalysisStepEvaluation(
            double evaluation,
            double bar,
            int depth,
            List<EngineLineDto> lines,
            DeepAnalysisResult deepAnalysisResult,
            boolean cancelled) {

        private static AnalysisStepEvaluation cancelledResult() {
            return new AnalysisStepEvaluation(0.0, 0.5, 0, List.of(), null, true);
        }
    }
}
