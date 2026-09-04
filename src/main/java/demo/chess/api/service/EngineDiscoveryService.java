package demo.chess.api.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import demo.chess.definitions.engines.UciEngineDefinition;
import demo.chess.definitions.engines.UciEngineInspector;

/**
 * Discovers executable UCI engines from a system directory.
 *
 * For safety, automatic discovery only considers executables whose file name
 * contains "stockfish" or "lc0" (case-insensitive). This name filter is
 * applied before any candidate process is started. Matching candidates still
 * have to complete the UCI handshake via {@link UciEngineInspector}.
 * Symlinks that resolve to the same executable are de-duplicated by real path.
 */
@Service
public class EngineDiscoveryService {

    public static final String DIRECTORY_PROPERTY = "chess.engine.discovery.directory";
    public static final String DEFAULT_DIRECTORY = "/usr/games";
    public static final String PREFERRED_EXECUTABLE = "stockfish";

    private static final Log logger = LogFactory.getLog(EngineDiscoveryService.class);

    private final Path discoveryDirectory;

    public EngineDiscoveryService() {
        this.discoveryDirectory = resolveDiscoveryDirectory();
    }

    public Path getDiscoveryDirectory() {
        return discoveryDirectory;
    }

    public String getPreferredEnginePath() {
        return discoveryDirectory.resolve(PREFERRED_EXECUTABLE)
                .toAbsolutePath()
                .normalize()
                .toString();
    }

    public List<UciEngineDefinition> discover() {
        if (!Files.isDirectory(discoveryDirectory)) {
            logger.info("Engine discovery directory does not exist: " + discoveryDirectory);
            return List.of();
        }

        List<Path> candidates;
        try (var stream = Files.list(discoveryDirectory)) {
            candidates = stream
                    .filter(Files::isRegularFile)
                    .filter(Files::isExecutable)
                    .filter(this::isAllowedDiscoveryCandidate)
                    .sorted(candidateComparator())
                    .toList();
        } catch (IOException e) {
            logger.warn("Could not list engine discovery directory " + discoveryDirectory + ": " + e.getMessage());
            return List.of();
        }

        Map<Path, UciEngineDefinition> definitionsByRealPath = new LinkedHashMap<>();
        for (Path candidate : candidates) {
            Path normalizedCandidate = candidate.toAbsolutePath().normalize();
            Path realPath;
            try {
                realPath = normalizedCandidate.toRealPath();
            } catch (IOException e) {
                logger.debug("Skipping unreadable engine candidate " + normalizedCandidate + ": " + e.getMessage());
                continue;
            }

            if (definitionsByRealPath.containsKey(realPath)) {
                logger.debug("Skipping duplicate engine candidate " + normalizedCandidate
                        + " because it resolves to " + realPath);
                continue;
            }

            try {
                UciEngineDefinition inspected = UciEngineInspector.inspect(normalizedCandidate.toString());
                definitionsByRealPath.put(realPath, inspected);
                logger.info("Discovered UCI engine " + inspected.getEngineName()
                        + " at " + normalizedCandidate);
            } catch (Exception e) {
                logger.debug("Ignoring allowed discovery candidate that is not a responsive UCI engine: "
                        + normalizedCandidate + " (" + e.getMessage() + ")");
            }
        }

        return new ArrayList<>(definitionsByRealPath.values());
    }

    private boolean isAllowedDiscoveryCandidate(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }
        String normalizedName = fileName.toString().toLowerCase(Locale.ROOT);
        return normalizedName.contains("stockfish") || normalizedName.contains("lc0");
    }

    private Comparator<Path> candidateComparator() {
        return Comparator
                .comparing((Path path) -> !PREFERRED_EXECUTABLE.equalsIgnoreCase(path.getFileName().toString()))
                .thenComparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER);
    }

    private Path resolveDiscoveryDirectory() {
        String configured = System.getProperty(DIRECTORY_PROPERTY);
        String value = configured == null || configured.isBlank()
                ? DEFAULT_DIRECTORY
                : configured.trim();
        return Path.of(value).toAbsolutePath().normalize();
    }
}
