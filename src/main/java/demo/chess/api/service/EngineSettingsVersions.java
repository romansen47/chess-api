package demo.chess.api.service;

/** Tracks configuration and role-specific runtime invalidation versions. */
final class EngineSettingsVersions {

    private long configuration = 1L;
    private long whitePlayer = 1L;
    private long blackPlayer = 1L;
    private long evaluation = 1L;

    void configurationChanged() {
        configuration++;
    }

    void configurationChanged(
            boolean whitePlayerChanged,
            boolean blackPlayerChanged,
            boolean evaluationChanged) {
        if (whitePlayerChanged) whitePlayer++;
        if (blackPlayerChanged) blackPlayer++;
        if (evaluationChanged) evaluation++;
        configuration++;
    }

    void allRuntimeRolesChanged() {
        configurationChanged(true, true, true);
    }

    void profileChanged(
            String profileId,
            String whiteProfileId,
            String blackProfileId,
            String evaluationProfileId) {
        configurationChanged(
                profileId.equals(whiteProfileId),
                profileId.equals(blackProfileId),
                profileId.equals(evaluationProfileId));
    }

    long configuration() {
        return configuration;
    }

    long whitePlayer() {
        return whitePlayer;
    }

    long blackPlayer() {
        return blackPlayer;
    }

    long evaluation() {
        return evaluation;
    }
}
