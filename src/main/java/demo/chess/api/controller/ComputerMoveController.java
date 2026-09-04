package demo.chess.api.controller;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.MoveResultDto;
import demo.chess.api.service.ComputerMoveService;
import demo.chess.definitions.Color;
import demo.chess.definitions.engines.impl.NoMoveFoundException;

@RestController
@RequestMapping("/api")
public class ComputerMoveController {

    private final ComputerMoveService computerMoveService;

    /**
     * Creates a new ComputerMoveController instance.
     * @param computerMoveService the computer move service
     */
    public ComputerMoveController(ComputerMoveService computerMoveService) {
        this.computerMoveService = computerMoveService;
    }

    /**
     * Performs the make computer move operation.
     * @return the result of the operation
     */
    @PostMapping("/computer-move")
    public ResponseEntity<MoveResultDto> makeComputerMove() throws NoMoveFoundException, IOException, InterruptedException, ExecutionException {
        try {
            MoveResultDto result = computerMoveService.makeComputerMove();
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.badRequest().body(result);
        } catch (NoMoveFoundException e) {
            return ResponseEntity.badRequest().body(
                    new MoveResultDto(false, e.getMessage(), null, null, null, null));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().body(
                    new MoveResultDto(false, "Interrupted while waiting for engine move", null, null, null, null));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(
                    new MoveResultDto(false, "I/O error while applying engine move", null, null, null, null));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(
                    new MoveResultDto(false, "Engine move failed: " + e.getMessage(), null, null, null, null));
        }
    }

    /**
     * Returns whether this object can cel computer move.
     * @param side the side
     * @return true when the condition is satisfied; otherwise false
     */
    @PostMapping("/computer-move/cancel")
    public ResponseEntity<Map<String, Object>> cancelComputerMove(@RequestParam String side) {
        Color color = parseSide(side);
        if (color == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Unsupported side: " + side));
        }

        computerMoveService.cancelPlayerEngine(color);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "side", color.name().toLowerCase(Locale.ROOT)));
    }

    /**
     * Parses the side.
     * @param side the side
     * @return the result of the operation
     */
    private Color parseSide(String side) {
        if (side == null) {
            return null;
        }

        String normalized = side.trim().toLowerCase(Locale.ROOT);
        if ("white".equals(normalized)) {
            return Color.WHITE;
        }
        if ("black".equals(normalized)) {
            return Color.BLACK;
        }

        return null;
    }
}
