package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.ObjectMapper;

import demo.chess.api.dto.EngineConfigOverviewDto;
import demo.chess.api.dto.EngineDefinitionDto;
import demo.chess.api.dto.EngineProfileDto;
import demo.chess.api.engine.NativeEngineRole;
import demo.chess.definitions.ChessStartingPosition;

class DeepAnalysisNativeFallbackTest {

    private static final String DIRECTORY_PROPERTY = "chess.engine.discovery.directory";
    private static final String STORE_PROPERTY = "chess.engine.config.file";

    @TempDir
    Path tempDir;

    private String previousDirectoryProperty;
    private String previousStoreProperty;

    @AfterEach
    void restoreProperties() {
        restoreProperty(DIRECTORY_PROPERTY, previousDirectoryProperty);
        restoreProperty(STORE_PROPERTY, previousStoreProperty);
    }

    @Test
    void fallsBackToAnotherWorkingNativeProfileBeforeReportingBrowserOnlyMode() throws Exception {
        Path games = Files.createDirectories(tempDir.resolve("games"));
        createUciEngine(games.resolve("stockfish"), "Engine A", false);
        createUciEngine(games.resolve("lc0"), "Engine B", false);
        configureProperties(games);

        EngineSettingsService settings = new EngineSettingsService(
                new ObjectMapper(), new EngineDiscoveryService());
        EngineRuntimeSelectionService runtime = new EngineRuntimeSelectionService(settings);
        EngineAvailabilityService availability = new EngineAvailabilityService(runtime, settings);

        EngineConfigOverviewDto overview = settings.getOverview();
        String defaultProfileId = settings.getDefaultDeepAnalysisProfileId();
        EngineProfileDto defaultProfile = overview.getProfiles().stream()
                .filter(profile -> defaultProfileId.equals(profile.getId()))
                .findFirst()
                .orElseThrow();
        EngineDefinitionDto defaultEngine = overview.getEngines().stream()
                .filter(engine -> defaultProfile.getEngineId().equals(engine.getId()))
                .findFirst()
                .orElseThrow();
        Files.delete(Path.of(defaultEngine.getEngine()));

        String fallbackProfileId = availability
                .findAvailableDeepAnalysisProfileId(defaultProfileId)
                .orElseThrow();

        assertNotEquals(defaultProfileId, fallbackProfileId);
        assertTrue(availability.getAvailability(NativeEngineRole.DEEP_ANALYSIS).available());
    }

    @Test
    void chess960SkipsResponsiveProfileWithoutChess960Capability() throws Exception {
        Path games = Files.createDirectories(tempDir.resolve("games"));
        Path classical = createUciEngine(games.resolve("stockfish"), "Classical Engine", false);
        Path chess960 = createUciEngine(games.resolve("lc0"), "Chess960 Engine", true);
        configureProperties(games);

        EngineSettingsService settings = new EngineSettingsService(
                new ObjectMapper(), new EngineDiscoveryService());
        EngineRuntimeSelectionService runtime = new EngineRuntimeSelectionService(settings);
        EngineAvailabilityService availability = new EngineAvailabilityService(runtime, settings);
        EngineConfigOverviewDto overview = settings.getOverview();

        String classicalProfileId = profileIdForPath(overview, classical);
        String chess960ProfileId = profileIdForPath(overview, chess960);

        String resolved = availability.findAvailableDeepAnalysisProfileId(
                        classicalProfileId,
                        ChessStartingPosition.of(0))
                .orElseThrow();

        assertEquals(chess960ProfileId, resolved);
        assertTrue(availability.getAvailability(
                NativeEngineRole.DEEP_ANALYSIS,
                ChessStartingPosition.of(0)).available());
    }

    private String profileIdForPath(EngineConfigOverviewDto overview, Path path) {
        String engineId = overview.getEngines().stream()
                .filter(engine -> Path.of(engine.getEngine()).toAbsolutePath().normalize()
                        .equals(path.toAbsolutePath().normalize()))
                .map(EngineDefinitionDto::getId)
                .findFirst()
                .orElseThrow();
        return overview.getProfiles().stream()
                .filter(profile -> engineId.equals(profile.getEngineId()))
                .map(EngineProfileDto::getId)
                .findFirst()
                .orElseThrow();
    }

    private Path createUciEngine(Path path, String name, boolean chess960) throws IOException {
        String chess960Option = chess960
                ? "      echo \"option name UCI_Chess960 type check default false\"\n"
                : "";
        String script = "#!/bin/sh\n"
                + "while IFS= read -r command; do\n"
                + "  case \"$command\" in\n"
                + "    uci)\n"
                + "      echo \"id name " + name + "\"\n"
                + "      echo \"id author Test\"\n"
                + chess960Option
                + "      echo \"uciok\"\n"
                + "      ;;\n"
                + "    quit) exit 0 ;;\n"
                + "  esac\n"
                + "done\n";
        Files.writeString(path, script, StandardCharsets.UTF_8);
        assertTrue(path.toFile().setExecutable(true, false) || Files.isExecutable(path));
        return path;
    }

    private void configureProperties(Path games) {
        previousDirectoryProperty = System.getProperty(DIRECTORY_PROPERTY);
        previousStoreProperty = System.getProperty(STORE_PROPERTY);
        System.setProperty(DIRECTORY_PROPERTY, games.toAbsolutePath().normalize().toString());
        System.setProperty(STORE_PROPERTY, tempDir.resolve("engine-configs.json").toString());
    }

    private void restoreProperty(String name, String value) {
        if (value == null) System.clearProperty(name);
        else System.setProperty(name, value);
    }
}
