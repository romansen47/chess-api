package demo.chess.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import demo.chess.api.dto.BrowserLineDisplayRequestDto;
import demo.chess.api.dto.EngineLineDto;
import demo.chess.definitions.ChessStartingPosition;
import demo.chess.definitions.engines.EngineLine;
import demo.chess.definitions.engines.impl.NoMoveFoundException;
import demo.chess.game.DummyGame;
import demo.chess.game.impl.Simulation;
import demo.chess.load.GameLoader;

/** Enriches browser-engine UCI lines without starting a backend engine. */
@Service
public class BrowserLineDisplayService {

    private static final int MAX_POSITION_MOVES = 2048;
    private static final int MAX_LINES = 6;
    private static final int MAX_LINE_LENGTH = 4096;

    private final EngineLineDisplayService lineDisplayService;
    private final GameLoader gameLoader = new GameLoader();

    /**
     * Creates a browser-line display service.
     * @param lineDisplayService canonical engine-line presentation converter
     */
    public BrowserLineDisplayService(EngineLineDisplayService lineDisplayService) {
        this.lineDisplayService = lineDisplayService;
    }

    /**
     * Reconstructs the analysed position and enriches every raw line.
     * @param request browser-engine display request
     * @return lines containing SAN, positions and move arrows
     * @throws NoMoveFoundException when the position history is illegal
     * @throws IOException when position reconstruction fails
     */
    public List<EngineLineDto> enrich(BrowserLineDisplayRequestDto request)
            throws NoMoveFoundException, IOException {
        validate(request);
        ChessStartingPosition startingPosition =
                ChessStartingPosition.fromInitialFen(request.initialFen());
        DummyGame base = Simulation.createDummySimulation(startingPosition);
        gameLoader.loadGame(copy(request.positionMoves()), base);

        List<EngineLineDto> result = new ArrayList<>(request.lines().size());
        for (EngineLineDto raw : request.lines()) {
            DummyGame lineGame = Simulation.forkDummyFrom(base.getMoveList());
            result.add(lineDisplayService.toDto(lineGame, new EngineLine(
                    raw.getEval(),
                    raw.getDepth(),
                    raw.getMateDistance(),
                    raw.getMoves())));
        }
        return List.copyOf(result);
    }

    private void validate(BrowserLineDisplayRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Browser line request must not be null");
        }
        if (request.positionMoves() == null || request.lines() == null) {
            throw new IllegalArgumentException("Position moves and engine lines are required");
        }
        if (request.positionMoves().size() > MAX_POSITION_MOVES) {
            throw new IllegalArgumentException("Position history is too long");
        }
        if (request.lines().size() > MAX_LINES) {
            throw new IllegalArgumentException("Too many browser engine lines");
        }
        for (EngineLineDto line : request.lines()) {
            if (line == null || line.getMoves() == null) {
                throw new IllegalArgumentException("Browser engine line must contain moves");
            }
            if (line.getMoves().length() > MAX_LINE_LENGTH) {
                throw new IllegalArgumentException("Browser engine line is too long");
            }
        }
    }

    private List<String> copy(List<String> moves) {
        return moves == null ? List.of() : List.copyOf(moves);
    }
}
