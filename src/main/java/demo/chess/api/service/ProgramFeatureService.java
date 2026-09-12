package demo.chess.api.service;

import org.springframework.stereotype.Service;

/**
 * Reads runtime feature switches from JVM system properties.
 */
@Service
public final class ProgramFeatureService {

    static final String DEBUG_MODE_PROPERTY = "debugMode";

    /**
     * Returns whether debug-only functionality is enabled.
     *
     * <p>The property is presence-based so both {@code -DdebugMode} and
     * {@code -DdebugMode=true} enable the mode. An explicit value of
     * {@code false} disables it.</p>
     *
     * @return true when debug mode is enabled
     */
    public boolean isDebugModeEnabled() {
        String value = System.getProperty(DEBUG_MODE_PROPERTY);
        return value != null
                && !"false".equalsIgnoreCase(value.trim());
    }
}
