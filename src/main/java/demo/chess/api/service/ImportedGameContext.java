package demo.chess.api.service;

import java.util.LinkedHashMap;
import java.util.Map;

import demo.chess.game.Game;
import demo.chess.notation.PgnMoveAnnotation;

/**
 * Mutable application context for the one PGN/database game selected for
 * analysis.
 *
 * <p>Keeping the imported game, its PGN tags, database identity and annotations
 * together prevents parallel service fields from drifting out of sync.</p>
 */
final class ImportedGameContext {

    private final Game game;
    private final Map<String, String> pgnTags;
    private Long databaseGameId;
    private Map<Integer, PgnMoveAnnotation> annotations;

    ImportedGameContext(
            Game game,
            Map<String, String> pgnTags,
            Long databaseGameId,
            Map<Integer, PgnMoveAnnotation> annotations) {
        if (game == null) throw new IllegalArgumentException("game must not be null");
        this.game = game;
        this.pgnTags = pgnTags != null ? new LinkedHashMap<>(pgnTags) : new LinkedHashMap<>();
        this.databaseGameId = databaseGameId;
        this.annotations = annotations != null
                ? new LinkedHashMap<>(annotations)
                : new LinkedHashMap<>();
    }

    Game game() {
        return game;
    }

    Map<String, String> pgnTagsCopy() {
        return new LinkedHashMap<>(pgnTags);
    }

    Long databaseGameId() {
        return databaseGameId;
    }

    void setDatabaseGameId(Long databaseGameId) {
        this.databaseGameId = databaseGameId;
    }

    Map<Integer, PgnMoveAnnotation> annotationsCopy() {
        return new LinkedHashMap<>(annotations);
    }

    void setAnnotations(Map<Integer, PgnMoveAnnotation> annotations) {
        this.annotations = annotations != null
                ? new LinkedHashMap<>(annotations)
                : new LinkedHashMap<>();
    }
}
