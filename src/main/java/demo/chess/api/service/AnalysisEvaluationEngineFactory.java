package demo.chess.api.service;

import org.springframework.stereotype.Component;

import demo.chess.api.engine.NativeEngineRole;
import demo.chess.api.exception.NativeEngineUnavailableException;
import demo.chess.definitions.engines.impl.EvaluationUciEngine;

/**
 * Creates evaluation-engine processes used by analysis-mode services.
 *
 * <p>The factory keeps engine construction injectable so service tests can use
 * mocked engines and never depend on a real UCI executable.</p>
 */
@Component
public class AnalysisEvaluationEngineFactory {

    public EvaluationUciEngine create(String enginePath, String managementLabel) {
        try {
            EvaluationUciEngine engine = new EvaluationUciEngine(enginePath);
            engine.setManagementLabel(managementLabel);
            return engine;
        } catch (Exception e) {
            throw NativeEngineUnavailableException.startFailure(
                    NativeEngineRole.EVALUATION,
                    e);
        }
    }
}
