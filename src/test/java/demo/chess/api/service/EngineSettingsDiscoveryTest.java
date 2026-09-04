package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import demo.chess.api.dto.EngineConfigOverviewDto;
import demo.chess.api.dto.EngineDefinitionDto;
import demo.chess.api.dto.EngineProfileDto;

class EngineSettingsDiscoveryTest {

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
    void discoversOnlyAllowedResponsiveUciEnginesAndPrefersStockfishAsFallback() throws Exception {
        Path games = Files.createDirectories(tempDir.resolve("games"));
        Path stockfish = createUciEngine(games.resolve("stockfish"), "Stockfish Test", 16);
        Path lc0 = createUciEngine(games.resolve("lc0"), "Lc0 Test", 64);

        // This is intentionally a perfectly responsive UCI engine. It must still
        // never be started/imported by auto-discovery because its name is not
        // explicitly allowed.
        createUciEngine(games.resolve("glaurung"), "Glaurung Test", 32);
        createNonUciExecutable(games.resolve("some-game"));

        try {
            Files.createSymbolicLink(games.resolve("stockfish-alias"), stockfish.getFileName());
        } catch (UnsupportedOperationException | IOException | SecurityException ignored) {
            // Symlink creation can be unavailable on some test environments.
        }

        configureProperties(games);

        EngineSettingsService service = new EngineSettingsService(
                new ObjectMapper(),
                new EngineDiscoveryService());
        EngineConfigOverviewDto overview = service.getOverview();

        assertEquals(2, overview.getEngines().size());
        assertEquals(2, overview.getProfiles().size());

        EngineProfileDto fallbackProfile = overview.getProfiles().stream()
                .filter(profile -> profile.getId().equals(overview.getFallbackProfileId()))
                .findFirst()
                .orElseThrow();
        EngineDefinitionDto fallbackEngine = overview.getEngines().stream()
                .filter(engine -> engine.getId().equals(fallbackProfile.getEngineId()))
                .findFirst()
                .orElseThrow();

        assertEquals(stockfish.toAbsolutePath().normalize().toString(), fallbackEngine.getEngine());
        assertEquals("Stockfish Test", fallbackEngine.getEngineName());
        assertEquals("16", fallbackEngine.getOptions().get("Hash").getDefaultValue());

        assertEquals(overview.getFallbackProfileId(), overview.getDefaults().getWhitePlayerProfileId());
        assertEquals(overview.getFallbackProfileId(), overview.getDefaults().getBlackPlayerProfileId());
        assertEquals(overview.getFallbackProfileId(), overview.getDefaults().getEvaluationProfileId());
        assertEquals(overview.getFallbackProfileId(), overview.getDefaults().getDeepAnalysisProfileId());

        assertTrue(overview.getEngines().stream().anyMatch(engine ->
                engine.getEngine().equals(lc0.toAbsolutePath().normalize().toString())));
        assertTrue(overview.getEngines().stream().noneMatch(engine -> engine.getEngine().endsWith("glaurung")));
        assertTrue(overview.getEngines().stream().noneMatch(engine -> engine.getEngine().endsWith("some-game")));
    }

    @Test
    void manualScanAddsOnlyNewAllowedEnginesAndDoesNotChangeAssignments() throws Exception {
        Path games = Files.createDirectories(tempDir.resolve("games"));
        createUciEngine(games.resolve("stockfish"), "Stockfish Test", 16);
        configureProperties(games);

        EngineSettingsService service = new EngineSettingsService(
                new ObjectMapper(),
                new EngineDiscoveryService());
        EngineConfigOverviewDto before = service.getOverview();
        String fallbackBefore = before.getFallbackProfileId();

        createUciEngine(games.resolve("lc0-new"), "New Lc0 Engine", 32);
        createUciEngine(games.resolve("new-engine"), "Ignored Engine", 48);
        EngineConfigOverviewDto after = service.discoverSystemEngines();

        assertEquals(2, after.getEngines().size());
        assertEquals(2, after.getProfiles().size());
        assertEquals(fallbackBefore, after.getFallbackProfileId());
        assertEquals(fallbackBefore, after.getDefaults().getWhitePlayerProfileId());
        assertEquals(fallbackBefore, after.getDefaults().getBlackPlayerProfileId());
        assertEquals(fallbackBefore, after.getDefaults().getEvaluationProfileId());
        assertEquals(fallbackBefore, after.getDefaults().getDeepAnalysisProfileId());
        assertTrue(after.getEngines().stream().noneMatch(engine -> engine.getEngine().endsWith("new-engine")));

        EngineConfigOverviewDto secondScan = service.discoverSystemEngines();
        assertEquals(2, secondScan.getEngines().size());
        assertEquals(2, secondScan.getProfiles().size());
        assertNotNull(secondScan.getFallbackProfileId());
    }

    @Test
    void deletingEngineRemovesAssociatedProfilesAndRepairsFallbackAssignments() throws Exception {
        Path games = Files.createDirectories(tempDir.resolve("games"));
        createUciEngine(games.resolve("stockfish"), "Stockfish Test", 16);
        createUciEngine(games.resolve("lc0"), "Lc0 Test", 32);
        configureProperties(games);

        EngineSettingsService service = new EngineSettingsService(
                new ObjectMapper(),
                new EngineDiscoveryService());
        EngineConfigOverviewDto before = service.getOverview();

        EngineProfileDto oldFallbackProfile = before.getProfiles().stream()
                .filter(profile -> profile.getId().equals(before.getFallbackProfileId()))
                .findFirst()
                .orElseThrow();
        String deletedEngineId = oldFallbackProfile.getEngineId();

        service.deleteEngine(deletedEngineId);
        EngineConfigOverviewDto after = service.getOverview();

        assertEquals(1, after.getEngines().size());
        assertEquals(1, after.getProfiles().size());
        assertTrue(after.getEngines().stream().noneMatch(engine -> engine.getId().equals(deletedEngineId)));
        assertTrue(after.getProfiles().stream().noneMatch(profile -> profile.getEngineId().equals(deletedEngineId)));

        EngineProfileDto newFallbackProfile = after.getProfiles().stream()
                .filter(profile -> profile.getId().equals(after.getFallbackProfileId()))
                .findFirst()
                .orElseThrow();
        assertEquals(after.getEngines().get(0).getId(), newFallbackProfile.getEngineId());
        assertEquals(after.getFallbackProfileId(), after.getDefaults().getWhitePlayerProfileId());
        assertEquals(after.getFallbackProfileId(), after.getDefaults().getBlackPlayerProfileId());
        assertEquals(after.getFallbackProfileId(), after.getDefaults().getEvaluationProfileId());
        assertEquals(after.getFallbackProfileId(), after.getDefaults().getDeepAnalysisProfileId());
    }

    private void configureProperties(Path games) {
        previousDirectoryProperty = System.getProperty(DIRECTORY_PROPERTY);
        previousStoreProperty = System.getProperty(STORE_PROPERTY);
        System.setProperty(DIRECTORY_PROPERTY, games.toAbsolutePath().normalize().toString());
        System.setProperty(STORE_PROPERTY, tempDir.resolve("engine-configs.json").toString());
    }

    private Path createUciEngine(Path path, String name, int hashDefault) throws IOException {
        String script = "#!/bin/sh\n"
                + "while IFS= read -r command; do\n"
                + "  case \"$command\" in\n"
                + "    uci)\n"
                + "      echo \"id name " + name + "\"\n"
                + "      echo \"id author Test\"\n"
                + "      echo \"option name Hash type spin default " + hashDefault + " min 1 max 1048576\"\n"
                + "      echo \"uciok\"\n"
                + "      ;;\n"
                + "    quit) exit 0 ;;\n"
                + "  esac\n"
                + "done\n";
        Files.writeString(path, script, StandardCharsets.UTF_8);
        assertTrue(path.toFile().setExecutable(true, false) || Files.isExecutable(path));
        return path;
    }

    private void createNonUciExecutable(Path path) throws IOException {
        Files.writeString(path, "#!/bin/sh\necho not-a-uci-engine\n", StandardCharsets.UTF_8);
        assertTrue(path.toFile().setExecutable(true, false) || Files.isExecutable(path));
    }

    private void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
