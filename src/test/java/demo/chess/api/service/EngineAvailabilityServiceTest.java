package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import demo.chess.api.engine.NativeEngineAvailability;
import demo.chess.api.engine.NativeEngineAvailabilityReason;
import demo.chess.api.engine.NativeEngineRole;
import demo.chess.definitions.ChessStartingPosition;

/**
 * Verifies the native-engine availability model independently of REST.
 */
class EngineAvailabilityServiceTest {

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
    void reportsAllRolesAsNotConfiguredWithoutNativeEngines() throws Exception {
        TestContext context = createContext(Files.createDirectories(tempDir.resolve("games")));

        for (NativeEngineRole role : NativeEngineRole.values()) {
            assertAvailability(
                    context.availabilityService().getAvailability(role),
                    role,
                    false,
                    false,
                    NativeEngineAvailabilityReason.NOT_CONFIGURED);
        }
    }

    @Test
    void reportsAllRolesAsAvailableForResponsiveAssignedEngine() throws Exception {
        Path games = Files.createDirectories(tempDir.resolve("games"));
        createUciEngine(games.resolve("stockfish"), "Stockfish Test");
        TestContext context = createContext(games);

        var availabilities = context.availabilityService().getAvailabilities();
        assertEquals(NativeEngineRole.values().length, availabilities.size());

        for (NativeEngineRole role : NativeEngineRole.values()) {
            assertAvailability(
                    availabilities.get(role),
                    role,
                    true,
                    true,
                    NativeEngineAvailabilityReason.AVAILABLE);
        }
    }

    @Test
    void responsiveEngineWithoutChess960IsUnavailableForEveryPosition() throws Exception {
        Path games = Files.createDirectories(tempDir.resolve("games"));
        createUciEngine(games.resolve("stockfish"), "Classical UCI", false);
        TestContext context = createContext(games);

        assertAvailability(
                context.availabilityService().getAvailability(
                        NativeEngineRole.EVALUATION,
                        ChessStartingPosition.of(0)),
                NativeEngineRole.EVALUATION,
                true,
                false,
                NativeEngineAvailabilityReason.CHESS960_UNSUPPORTED);

        assertAvailability(
                context.availabilityService().getAvailability(NativeEngineRole.EVALUATION),
                NativeEngineRole.EVALUATION,
                true,
                false,
                NativeEngineAvailabilityReason.CHESS960_UNSUPPORTED);
    }

    @Test
    void engineAdvertisingChess960IsAvailableForNonStandardPosition() throws Exception {
        Path games = Files.createDirectories(tempDir.resolve("games"));
        createUciEngine(games.resolve("stockfish"), "Chess960 UCI", true);
        TestContext context = createContext(games);

        assertAvailability(
                context.availabilityService().getAvailability(
                        NativeEngineRole.EVALUATION,
                        ChessStartingPosition.of(0)),
                NativeEngineRole.EVALUATION,
                true,
                true,
                NativeEngineAvailabilityReason.AVAILABLE);
    }

    @Test
    void reportsMissingExecutableAfterConfiguredEngineIsDeleted() throws Exception {
        Path games = Files.createDirectories(tempDir.resolve("games"));
        Path engine = createUciEngine(games.resolve("stockfish"), "Stockfish Test");
        TestContext context = createContext(games);

        Files.delete(engine);

        assertAvailability(
                context.availabilityService().getAvailability(NativeEngineRole.EVALUATION),
                NativeEngineRole.EVALUATION,
                true,
                false,
                NativeEngineAvailabilityReason.EXECUTABLE_NOT_FOUND);
    }

    @Test
    void reportsConfiguredEngineThatIsNoLongerExecutable() throws Exception {
        Path games = Files.createDirectories(tempDir.resolve("games"));
        Path engine = createUciEngine(games.resolve("stockfish"), "Stockfish Test");
        TestContext context = createContext(games);

        boolean changed = engine.toFile().setExecutable(false, false);
        assumeTrue(changed && !Files.isExecutable(engine));

        assertAvailability(
                context.availabilityService().getAvailability(NativeEngineRole.EVALUATION),
                NativeEngineRole.EVALUATION,
                true,
                false,
                NativeEngineAvailabilityReason.NOT_EXECUTABLE);
    }

    @Test
    void reportsConfiguredExecutableThatNoLongerSpeaksUci() throws Exception {
        Path games = Files.createDirectories(tempDir.resolve("games"));
        Path engine = createUciEngine(games.resolve("stockfish"), "Stockfish Test");
        TestContext context = createContext(games);

        Files.writeString(
                engine,
                "#!/bin/sh\necho not-a-uci-engine\n",
                StandardCharsets.UTF_8);
        assertTrue(Files.isExecutable(engine));

        assertAvailability(
                context.availabilityService().getAvailability(NativeEngineRole.EVALUATION),
                NativeEngineRole.EVALUATION,
                true,
                false,
                NativeEngineAvailabilityReason.UCI_UNRESPONSIVE);
    }

    @Test
    void evaluationRuntimeOverrideDoesNotChangeOtherRoleAssignments() throws Exception {
        Path games = Files.createDirectories(tempDir.resolve("games"));
        Path stockfish = createUciEngine(games.resolve("stockfish"), "Stockfish Test");
        Path lc0 = createUciEngine(games.resolve("lc0"), "Lc0 Test");
        TestContext context = createContext(games);

        EngineConfigOverviewDto overview = context.settingsService().getOverview();
        String lc0EngineId = overview.getEngines().stream()
                .filter(engine -> samePath(engine, lc0))
                .map(EngineDefinitionDto::getId)
                .findFirst()
                .orElseThrow();
        String lc0ProfileId = overview.getProfiles().stream()
                .filter(profile -> lc0EngineId.equals(profile.getEngineId()))
                .map(EngineProfileDto::getId)
                .findFirst()
                .orElseThrow();

        context.runtimeSelectionService().setEvaluationProfileId(lc0ProfileId);
        Files.delete(lc0);

        assertAvailability(
                context.availabilityService().getAvailability(NativeEngineRole.EVALUATION),
                NativeEngineRole.EVALUATION,
                true,
                false,
                NativeEngineAvailabilityReason.EXECUTABLE_NOT_FOUND);

        assertAvailability(
                context.availabilityService().getAvailability(NativeEngineRole.WHITE_PLAYER),
                NativeEngineRole.WHITE_PLAYER,
                true,
                true,
                NativeEngineAvailabilityReason.AVAILABLE);
        assertAvailability(
                context.availabilityService().getAvailability(NativeEngineRole.BLACK_PLAYER),
                NativeEngineRole.BLACK_PLAYER,
                true,
                true,
                NativeEngineAvailabilityReason.AVAILABLE);
        assertAvailability(
                context.availabilityService().getAvailability(NativeEngineRole.DEEP_ANALYSIS),
                NativeEngineRole.DEEP_ANALYSIS,
                true,
                true,
                NativeEngineAvailabilityReason.AVAILABLE);

        assertTrue(Files.exists(stockfish));
    }

    private TestContext createContext(Path games) {
        configureProperties(games);

        EngineSettingsService settingsService = new EngineSettingsService(
                new ObjectMapper(),
                new EngineDiscoveryService());
        EngineRuntimeSelectionService runtimeSelectionService =
                new EngineRuntimeSelectionService(settingsService);
        EngineAvailabilityService availabilityService =
                new EngineAvailabilityService(runtimeSelectionService, settingsService);

        return new TestContext(
                settingsService,
                runtimeSelectionService,
                availabilityService);
    }

    private Path createUciEngine(Path path, String name) throws IOException {
        return createUciEngine(path, name, true);
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

    private boolean samePath(EngineDefinitionDto engine, Path expected) {
        return Path.of(engine.getEngine()).toAbsolutePath().normalize()
                .equals(expected.toAbsolutePath().normalize());
    }

    private void assertAvailability(
            NativeEngineAvailability actual,
            NativeEngineRole role,
            boolean configured,
            boolean available,
            NativeEngineAvailabilityReason reason) {
        assertEquals(role, actual.role());
        assertEquals(configured, actual.configured());
        assertEquals(available, actual.available());
        assertEquals(reason, actual.reason());
        if (available) {
            assertTrue(actual.configured());
        } else if (!configured) {
            assertFalse(actual.available());
        }
    }

    private void configureProperties(Path games) {
        previousDirectoryProperty = System.getProperty(DIRECTORY_PROPERTY);
        previousStoreProperty = System.getProperty(STORE_PROPERTY);
        System.setProperty(DIRECTORY_PROPERTY, games.toAbsolutePath().normalize().toString());
        System.setProperty(STORE_PROPERTY, tempDir.resolve("engine-configs.json").toString());
    }

    private void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private record TestContext(
            EngineSettingsService settingsService,
            EngineRuntimeSelectionService runtimeSelectionService,
            EngineAvailabilityService availabilityService) {
    }
}
