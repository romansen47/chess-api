package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import demo.chess.api.dto.EngineConfigOverviewDto;
import demo.chess.api.dto.EngineProfileDto;

class FallbackProfileUpdateTest {

    private static final String DIRECTORY_PROPERTY = "chess.engine.discovery.directory";
    private static final String STORE_PROPERTY = "chess.engine.config.file";

    @TempDir
    Path tempDir;

    private String previousDirectoryProperty;
    private String previousStoreProperty;

    /**
     * Restores system properties changed for test isolation.
     */
    @AfterEach
    void restoreProperties() {
        restoreProperty(DIRECTORY_PROPERTY, previousDirectoryProperty);
        restoreProperty(STORE_PROPERTY, previousStoreProperty);
    }

    /**
     * Verifies that the fallback role protects a profile from deletion without making its values immutable.
     */
    @Test
    void fallbackProfileCanBeEditedPersistedAndStillCannotBeDeleted() throws Exception {
        EngineSettingsService service = createService();
        EngineConfigOverviewDto initial = service.getOverview();
        String fallbackId = initial.getFallbackProfileId();
        EngineProfileDto fallback = initial.getProfiles().stream()
                .filter(profile -> fallbackId.equals(profile.getId()))
                .findFirst()
                .orElseThrow();

        long whiteVersion = service.getWhitePlayerVersion();
        long blackVersion = service.getBlackPlayerVersion();
        long evaluationVersion = service.getEvaluationVersion();

        EngineProfileDto update = new EngineProfileDto();
        update.setEngineId(fallback.getEngineId());
        update.setName("Customized fallback");
        update.setOptionValues(Map.of(
                "Hash", "128",
                "Ponder", "false"));

        EngineProfileDto saved = service.updateProfile(fallbackId, update);

        assertEquals(fallbackId, saved.getId());
        assertEquals("Customized fallback", saved.getName());
        assertEquals("128", saved.getOptionValues().get("Hash"));
        assertEquals("false", saved.getOptionValues().get("Ponder"));
        assertEquals(whiteVersion + 1, service.getWhitePlayerVersion());
        assertEquals(blackVersion + 1, service.getBlackPlayerVersion());
        assertEquals(evaluationVersion + 1, service.getEvaluationVersion());
        assertEquals(128, service.getWhitePlayerConfig().getIntOption("Hash", -1));
        assertThrows(IllegalArgumentException.class, () -> service.deleteProfile(fallbackId));

        EngineSettingsService restarted = new EngineSettingsService(
                new ObjectMapper(),
                new EngineDiscoveryService());
        EngineConfigOverviewDto restored = restarted.getOverview();
        EngineProfileDto restoredFallback = restored.getProfiles().stream()
                .filter(profile -> fallbackId.equals(profile.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(fallbackId, restored.getFallbackProfileId());
        assertEquals("Customized fallback", restoredFallback.getName());
        assertEquals("128", restoredFallback.getOptionValues().get("Hash"));
        assertEquals("false", restoredFallback.getOptionValues().get("Ponder"));
        assertEquals(128, restarted.getWhitePlayerConfig().getIntOption("Hash", -1));
    }

    /**
     * Creates an isolated settings service backed by a deterministic fake UCI engine.
     * @return the initialized service
     */
    private EngineSettingsService createService() throws IOException {
        Path games = Files.createDirectories(tempDir.resolve("games"));
        createUciEngine(games.resolve("stockfish"));
        configureProperties(games);
        return new EngineSettingsService(new ObjectMapper(), new EngineDiscoveryService());
    }

    /**
     * Writes an executable fake engine exposing representative configurable UCI options.
     * @param path the executable path
     */
    private void createUciEngine(Path path) throws IOException {
        String script = "#!/bin/sh\n"
                + "while IFS= read -r command; do\n"
                + "  case \"$command\" in\n"
                + "    uci)\n"
                + "      echo \"id name Fallback Test Engine\"\n"
                + "      echo \"id author Tests\"\n"
                + "      echo \"option name Hash type spin default 16 min 1 max 1024\"\n"
                + "      echo \"option name Ponder type check default true\"\n"
                + "      echo \"uciok\"\n"
                + "      ;;\n"
                + "    quit) exit 0 ;;\n"
                + "  esac\n"
                + "done\n";
        Files.writeString(path, script, StandardCharsets.UTF_8);
        assertTrue(path.toFile().setExecutable(true, false) || Files.isExecutable(path));
    }

    /**
     * Configures discovery and persistence paths for the current temporary directory.
     * @param games the discovery directory
     */
    private void configureProperties(Path games) {
        previousDirectoryProperty = System.getProperty(DIRECTORY_PROPERTY);
        previousStoreProperty = System.getProperty(STORE_PROPERTY);
        System.setProperty(DIRECTORY_PROPERTY, games.toAbsolutePath().normalize().toString());
        System.setProperty(STORE_PROPERTY, tempDir.resolve("engine-configs.json").toString());
    }

    /**
     * Restores one system property to its value before the test.
     * @param name the property name
     * @param value the previous property value
     */
    private void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
