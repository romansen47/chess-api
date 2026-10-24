package demo.chess.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import demo.chess.analysis.annotation.MoveAnnotation;
import demo.chess.analysis.annotation.MoveAnnotationKind;
import demo.chess.api.dto.MoveAnnotationDto;
import demo.chess.game.Game;
import demo.chess.game.impl.Simulation;

class MoveAnnotationDtoMapperTest {

    @Test
    void rendersReferencedMovesInDisplayNotation() {
        Game position = Simulation.createSimulation();
        MoveAnnotation annotation = new MoveAnnotation(
                MoveAnnotationKind.MISTAKE,
                0.40,
                12.0,
                null,
                "e2e4",
                "d2d4");

        MoveAnnotationDto dto =
                MoveAnnotationDtoMapper.toDto(annotation, position);

        assertEquals("e4", dto.getBestMove());
        assertEquals("d4", dto.getSecondBestMove());
    }
}
