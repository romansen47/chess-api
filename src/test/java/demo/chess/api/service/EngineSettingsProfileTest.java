package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import demo.chess.api.dto.EngineConfigOverviewDto;
import demo.chess.api.dto.EngineDefinitionDto;
import demo.chess.api.dto.EngineProfileAssignmentsDto;
import demo.chess.api.dto.EngineProfileDto;
import demo.chess.definitions.engines.UciEngineConfig;

class EngineSettingsProfileTest {

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
     * Verifies profile normalization and the runtime search-limit semantics used by deep analysis.
     */
    @Test
    void profileValuesAreNormalizedAndResolvedIntoRuntimeConfig() throws Exception {
        EngineSettingsService service = createService();
        EngineDefinitionDto engine = service.getOverview().getEngines().get(0);

        EngineProfileDto created = service.createProfile(profile(
                engine.getId(),
                " Tuned ",
                Map.of(
                        "Hash", " 128 ",
                        "Ponder", " FALSE ",
                        "Style", "Aggressive",
                        "SyzygyPath", "/tablebases")));

        assertEquals("Tuned", created.getName());
        assertEquals("128", created.getOptionValues().get("Hash"));
        assertEquals("false", created.getOptionValues().get("Ponder"));
        assertEquals("Aggressive", created.getOptionValues().get("Style"));
        assertEquals("/tablebases", created.getOptionValues().get("SyzygyPath"));
        assertFalse(created.getOptionValues().containsKey("Clear Hash"));

        UciEngineConfig runtime = service.getConfig(created.getId());
        assertEquals(128, runtime.getIntOption("Hash", -1));
        assertEquals("false", runtime.getStringOption("Ponder", "missing"));
        assertEquals("Aggressive", runtime.getStringOption("Style", "missing"));
        assertEquals("/tablebases", runtime.getStringOption("SyzygyPath", "missing"));

        UciEngineConfig depthLimited = service.getDeepAnalysisConfig(created.getId(), 8, 30);
        assertEquals(8, depthLimited.getDepth());
        assertEquals(0, depthLimited.getMoveTimeSeconds());

        UciEngineConfig timeLimited = service.getDeepAnalysisConfig(created.getId(), 0, 0);
        assertEquals(0, timeLimited.getDepth());
        assertEquals(1, timeLimited.getMoveTimeSeconds());
    }

    /**
     * Verifies that invalid, unknown and action-only UCI values cannot enter a reusable profile.
     */
    @Test
    void profileCreationRejectsInvalidEngineOptions() throws Exception {
        EngineSettingsService service = createService();
        String engineId = service.getOverview().getEngines().get(0).getId();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createProfile(profile(engineId, "Bad Hash", Map.of("Hash", "0"))));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.createProfile(profile(engineId, "Unknown", Map.of("NotSupported", "1"))));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.createProfile(profile(engineId, "Button", Map.of("Clear Hash", "now"))));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.createProfile(profile(engineId, "Combo", Map.of("Style", "Experimental"))));
    }

    /**
     * Verifies that changing an assigned profile invalidates only the runtime consumers using it.
     */
    @Test
    void updatingAssignedProfileBumpsRelevantRuntimeVersionsAndPersists() throws Exception {
        EngineSettingsService service = createService();
        EngineConfigOverviewDto initial = service.getOverview();
        String fallbackId = initial.getFallbackProfileId();
        String engineId = initial.getEngines().get(0).getId();

        EngineProfileDto custom = service.createProfile(profile(
                engineId,
                "Tournament",
                Map.of("Hash", "64", "Ponder", "false")));

        service.updateDefaultAssignments(new EngineProfileAssignmentsDto(
                custom.getId(),
                fallbackId,
                custom.getId(),
                fallbackId));

        long whiteVersion = service.getWhitePlayerVersion();
        long blackVersion = service.getBlackPlayerVersion();
        long evaluationVersion = service.getEvaluationVersion();
        long globalVersion = service.getVersion();

        EngineProfileDto changed = profile(
                engineId,
                "Tournament updated",
                Map.of("Hash", "256", "Ponder", "true"));
        service.updateProfile(custom.getId(), changed);

        assertEquals(whiteVersion + 1, service.getWhitePlayerVersion());
        assertEquals(blackVersion, service.getBlackPlayerVersion());
        assertEquals(evaluationVersion + 1, service.getEvaluationVersion());
        assertEquals(globalVersion + 1, service.getVersion());
        assertEquals(256, service.getWhitePlayerConfig().getIntOption("Hash", -1));
        assertEquals(256, service.toEvaluationEngineConfig().getIntOption("Hash", -1));

        EngineSettingsService restarted = new EngineSettingsService(
                new ObjectMapper(),
                new EngineDiscoveryService());
        EngineConfigOverviewDto restored = restarted.getOverview();

        assertEquals(custom.getId(), restored.getDefaults().getWhitePlayerProfileId());
        assertEquals(custom.getId(), restored.getDefaults().getEvaluationProfileId());
        assertEquals(256, restarted.getWhitePlayerConfig().getIntOption("Hash", -1));
        assertEquals("true", restarted.getWhitePlayerConfig().getStringOption("Ponder", "missing"));
    }

    /**
     * Verifies that protected profiles cannot be removed until their assignments are released.
     */
    @Test
    void profileDeletionHonorsFallbackAndDefaultAssignments() throws Exception {
        EngineSettingsService service = createService();
        EngineConfigOverviewDto initial = service.getOverview();
        String fallbackId = initial.getFallbackProfileId();
        String engineId = initial.getEngines().get(0).getId();

        assertThrows(IllegalArgumentException.class, () -> service.deleteProfile(fallbackId));

        EngineProfileDto custom = service.createProfile(profile(
                engineId,
                "Disposable",
                Map.of("Hash", "32")));
        service.updateDefaultAssignments(new EngineProfileAssignmentsDto(
                custom.getId(),
                fallbackId,
                fallbackId,
                fallbackId));

        assertThrows(IllegalArgumentException.class, () -> service.deleteProfile(custom.getId()));

        service.updateDefaultAssignments(new EngineProfileAssignmentsDto(
                fallbackId,
                fallbackId,
                fallbackId,
                fallbackId));
        service.deleteProfile(custom.getId());

        assertTrue(service.getOverview().getProfiles().stream()
                .noneMatch(profile -> custom.getId().equals(profile.getId())));
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
     * Creates a profile request with the supplied option overrides.
     * @param engineId the target engine id
     * @param name the profile name
     * @param values the requested option values
     * @return the profile request
     */
    private EngineProfileDto profile(String engineId, String name, Map<String, String> values) {
        EngineProfileDto dto = new EngineProfileDto();
        dto.setEngineId(engineId);
        dto.setName(name);
        dto.setOptionValues(new LinkedHashMap<>(values));
        return dto;
    }

    /**
     * Writes an executable fake engine exposing representative UCI option types.
     * @param path the executable path
     */
    private void createUciEngine(Path path) throws IOException {
        String script = "#!/bin/sh\n"
                + "while IFS= read -r command; do\n"
                + "  case \"$command\" in\n"
                + "    uci)\n"
                + "      echo \"id name Profile Test Engine\"\n"
                + "      echo \"id author Tests\"\n"
                + "      echo \"option name Hash type spin default 16 min 1 max 1024\"\n"
                + "      echo \"option name Ponder type check default true\"\n"
                + "      echo \"option name Style type combo default Normal var Normal var Aggressive\"\n"
                + "      echo \"option name Clear Hash type button\"\n"
                + "      echo \"option name SyzygyPath type string default <empty>\"\n"
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
