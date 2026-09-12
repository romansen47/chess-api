package demo.chess.api.dto;

import java.util.List;

/**
 * Complete annotation state submitted by the frontend.
 *
 * @param annotations annotations to persist
 * @param whiteComputer whether White is computer-controlled for a live game export
 * @param blackComputer whether Black is computer-controlled for a live game export
 */
public record GameAnnotationsRequestDto(
        List<GameAnnotationDto> annotations,
        boolean whiteComputer,
        boolean blackComputer) {

    public GameAnnotationsRequestDto {
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }
}
