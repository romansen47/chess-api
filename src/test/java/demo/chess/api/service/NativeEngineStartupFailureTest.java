package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.ObjectMapper;

import demo.chess.api.engine.NativeEngineRole;
import demo.chess.api.exception.NativeEngineUnavailableException;

/**
 * Verifies the race where a configured native engine becomes unusable before
 * the operation actually starts its UCI process.
 */
class NativeEngineStartupFailureTest {

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
    void liveEvaluationReportsConfiguredEngineThatDisappearedBeforeStart() throws Exception {
        TestContext context = createConfiguredContext();
        Files.delete(context.enginePath());

        EvaluationService service = new EvaluationService(
                new GameService(),
                context.runtimeSelectionService(),
                new LiveEvaluationStreamService(),
                new EngineLineDisplayService());

        NativeEngineUnavailableException exception = assertThrows(
                NativeEngineUnavailableException.class,
                service::getEvaluation);

        assertStartFailure(exception, NativeEngineRole.EVALUATION);
    }

    @Test
    void liveEvaluationReportsConfiguredEngineThatStopsSpeakingUci() throws Exception {
        TestContext context = createConfiguredContext();
        Files.writeString(
                context.enginePath(),
                "#!/bin/sh\necho not-a-uci-engine\n",
                StandardCharsets.UTF_8);
        assertTrue(Files.isExecutable(context.enginePath()));

        EvaluationService service = new EvaluationService(
                new GameService(),
                context.runtimeSelectionService(),
                new LiveEvaluationStreamService(),
                new EngineLineDisplayService());

        NativeEngineUnavailableException exception = assertThrows(
                NativeEngineUnavailableException.class,
                service::getEvaluation);

        assertStartFailure(exception, NativeEngineRole.EVALUATION);
    }

    @Test
    void computerMoveReportsConfiguredPlayerEngineThatDisappearedBeforeStart() throws Exception {
        TestContext context = createConfiguredContext();
        GameService gameService = new GameService();
        gameService.startNewGame();
        Files.delete(context.enginePath());

        ComputerMoveService service = new ComputerMoveService(
                gameService,
                context.runtimeSelectionService());

        NativeEngineUnavailableException exception = assertThrows(
                NativeEngineUnavailableException.class,
                service::makeComputerMove);

        assertStartFailure(exception, NativeEngineRole.WHITE_PLAYER);
    }

    @Test
    void deepAnalysisReportsUnavailableAfterAllNativeProfilesAreExhaustedWithoutStoppingLiveEvaluation()
            throws Exception {
        TestContext context = createConfiguredContext();
        Files.delete(context.enginePath());

        EvaluationService evaluationService = mock(EvaluationService.class);
        UciGameService uciGameService = mock(UciGameService.class);
        when(uciGameService.getAnalysisMoveListSnapshot()).thenReturn(java.util.List.of());

        AnalysisReplayService service = new AnalysisReplayService(
                new GameService(),
                context.settingsService(),
                evaluationService,
                uciGameService,
                new EngineLineDisplayService());

        NativeEngineUnavailableException exception = assertThrows(
                NativeEngineUnavailableException.class,
                () -> service.start(null));

        assertEquals(NativeEngineRole.DEEP_ANALYSIS, exception.getRole());
        verifyNoInteractions(evaluationService);
    }

    @Test
    void analysisEvaluationFactoryReportsStartupFailureWithEvaluationRole() {
        AnalysisEvaluationEngineFactory factory = new AnalysisEvaluationEngineFactory();

        NativeEngineUnavailableException exception = assertThrows(
                NativeEngineUnavailableException.class,
                () -> factory.create(
                        tempDir.resolve("missing-engine").toString(),
                        "analysis evaluation"));

        assertStartFailure(exception, NativeEngineRole.EVALUATION);
    }

    private TestContext createConfiguredContext() throws IOException {
        Path games = Files.createDirectories(tempDir.resolve("games"));
        Path engine = createUciEngine(games.resolve("stockfish"));
        configureProperties(games);

        EngineSettingsService settingsService = new EngineSettingsService(
                new ObjectMapper(),
                new EngineDiscoveryService());
        EngineRuntimeSelectionService runtimeSelectionService =
                new EngineRuntimeSelectionService(settingsService);

        assertTrue(runtimeSelectionService.findEvaluationConfig().isPresent());
        return new TestContext(settingsService, runtimeSelectionService, engine);
    }

    private Path createUciEngine(Path path) throws IOException {
        String script = "#!/bin/sh\n"
                + "while IFS= read -r command; do\n"
                + "  case \"$command\" in\n"
                + "    uci)\n"
                + "      echo \"id name Startup Test\"\n"
                + "      echo \"id author Test\"\n"
                + "      echo \"uciok\"\n"
                + "      ;;\n"
                + "    quit) exit 0 ;;\n"
                + "  esac\n"
                + "done\n";
        Files.writeString(path, script, StandardCharsets.UTF_8);
        assertTrue(path.toFile().setExecutable(true, false) || Files.isExecutable(path));
        return path;
    }

    private void assertStartFailure(
            NativeEngineUnavailableException exception,
            NativeEngineRole role) {
        assertEquals(role, exception.getRole());
        assertEquals(
                "Native engine for " + role.getDisplayName()
                        + " is configured but could not be started",
                exception.getMessage());
        assertNotNull(exception.getCause());
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
            Path enginePath) {
    }
}
