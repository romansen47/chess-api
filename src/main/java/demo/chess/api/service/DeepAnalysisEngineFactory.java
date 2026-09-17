package demo.chess.api.service;

import demo.chess.api.engine.NativeEngineRole;
import demo.chess.api.exception.NativeEngineUnavailableException;
import demo.chess.definitions.engines.DeepAnalysisEngine;
import demo.chess.definitions.engines.impl.DeepAnalysisUciEngine;

/** Creates the native UCI engine process used by deep-analysis replay. */
final class DeepAnalysisEngineFactory {

    /**
     * Starts a native deep-analysis engine and maps startup failures to the
     * stable API exception used by the capability boundary.
     *
     * @param enginePath executable path from the resolved engine profile
     * @return started deep-analysis engine
     */
    DeepAnalysisEngine create(String enginePath) {
        if (enginePath == null || enginePath.isBlank()) {
            throw NativeEngineUnavailableException.startFailure(
                    NativeEngineRole.DEEP_ANALYSIS,
                    new IllegalStateException("Deep analysis engine profile has no executable path"));
        }
        try {
            DeepAnalysisUciEngine engine = new DeepAnalysisUciEngine(enginePath.trim());
            engine.setManagementLabel("deep analysis");
            return engine;
        } catch (Exception ex) {
            throw NativeEngineUnavailableException.startFailure(NativeEngineRole.DEEP_ANALYSIS, ex);
        }
    }
}
