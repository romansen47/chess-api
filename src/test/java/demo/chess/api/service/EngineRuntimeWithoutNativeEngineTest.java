package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.ObjectMapper;

import demo.chess.api.dto.GameSettingsDto;
import demo.chess.api.exception.NativeEngineUnavailableException;
import demo.chess.api.exception.NativeEngineUnavailableException.Role;

/**
 * Verifies that application services can exist and reset normally when no
 * native UCI engine is configured. Actual engine-use error semantics belong to
 * the subsequent runtime error-handling step.
 */
class EngineRuntimeWithoutNativeEngineTest {

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
    void runtimeSelectionExpressesMissingNativeEnginesAsEmptyOptionals() throws Exception {
        EngineRuntimeSelectionService runtimeSelection = createRuntimeSelectionWithoutEngine();

        assertNull(runtimeSelection.getEffectiveWhitePlayerProfileId());
        assertNull(runtimeSelection.getEffectiveBlackPlayerProfileId());
        assertNull(runtimeSelection.getEffectiveEvaluationProfileId());

        assertTrue(runtimeSelection.findWhitePlayerConfig().isEmpty());
        assertTrue(runtimeSelection.findBlackPlayerConfig().isEmpty());
        assertTrue(runtimeSelection.findEvaluationConfig().isEmpty());

        assertTrue(runtimeSelection.findWhitePlayerEnginePath().isEmpty());
        assertTrue(runtimeSelection.findBlackPlayerEnginePath().isEmpty());
        assertTrue(runtimeSelection.findEvaluationEnginePath().isEmpty());
    }

    @Test
    void requiredRuntimeConfigsReportStableMissingEngineRoles() throws Exception {
        EngineRuntimeSelectionService runtimeSelection = createRuntimeSelectionWithoutEngine();

        assertUnavailable(Role.WHITE_PLAYER, runtimeSelection::requireWhitePlayerConfig);
        assertUnavailable(Role.BLACK_PLAYER, runtimeSelection::requireBlackPlayerConfig);
        assertUnavailable(Role.EVALUATION, runtimeSelection::requireEvaluationConfig);

        assertUnavailable(Role.WHITE_PLAYER, runtimeSelection::getWhitePlayerConfig);
        assertUnavailable(Role.BLACK_PLAYER, runtimeSelection::getBlackPlayerConfig);
        assertUnavailable(Role.EVALUATION, runtimeSelection::getEvaluationConfig);
    }

    @Test
    void liveEvaluationReportsMissingNativeEvaluationEngine() throws Exception {
        EngineRuntimeSelectionService runtimeSelection = createRuntimeSelectionWithoutEngine();
        EvaluationService evaluationService = new EvaluationService(
                new GameService(),
                runtimeSelection,
                new LiveEvaluationStreamService(),
                new EngineLineDisplayService());

        assertUnavailable(Role.EVALUATION, evaluationService::getEvaluation);
    }

    @Test
    void computerMoveReportsMissingNativePlayerEngine() throws Exception {
        EngineRuntimeSelectionService runtimeSelection = createRuntimeSelectionWithoutEngine();
        ComputerMoveService computerMoveService =
                new ComputerMoveService(new GameService(), runtimeSelection);

        assertUnavailable(Role.WHITE_PLAYER, computerMoveService::makeComputerMove);
    }

    @Test
    void deepAnalysisReportsMissingNativeEngineWithoutDefaultPathFallback() throws Exception {
        EngineRuntimeSelectionService runtimeSelection = createRuntimeSelectionWithoutEngine();
        EngineSettingsService settingsService = getSettingsService(runtimeSelection);
        EvaluationService evaluationService = mock(EvaluationService.class);
        UciGameService uciGameService = mock(UciGameService.class);
        when(uciGameService.getAnalysisMoveListSnapshot()).thenReturn(java.util.List.of());

        AnalysisReplayService replayService = new AnalysisReplayService(
                new GameService(),
                settingsService,
                evaluationService,
                uciGameService,
                new EngineLineDisplayService());

        assertUnavailable(Role.DEEP_ANALYSIS, () -> replayService.start(null));
    }

    @Test
    void engineServicesCanBeConstructedAndResetWithoutNativeEngine() throws Exception {
        EngineRuntimeSelectionService runtimeSelection = createRuntimeSelectionWithoutEngine();
        GameService gameService = new GameService();

        EvaluationService evaluationService = assertDoesNotThrow(() ->
                new EvaluationService(
                        gameService,
                        runtimeSelection,
                        new LiveEvaluationStreamService(),
                        new EngineLineDisplayService()));

        ComputerMoveService computerMoveService = assertDoesNotThrow(() ->
                new ComputerMoveService(gameService, runtimeSelection));

        assertDoesNotThrow(evaluationService::resetForNewGame);
        assertDoesNotThrow(computerMoveService::resetForNewGame);
        assertDoesNotThrow(() -> computerMoveService.cancelPlayerEngine(
                demo.chess.definitions.Color.WHITE));
        assertDoesNotThrow(() -> computerMoveService.cancelPlayerEngine(
                demo.chess.definitions.Color.BLACK));
    }

    @Test
    void newHumanGameStartsWithoutNativeEngine() throws Exception {
        EngineRuntimeSelectionService runtimeSelection = createRuntimeSelectionWithoutEngine();
        GameService gameService = new GameService();
        EvaluationService evaluationService = new EvaluationService(
                gameService,
                runtimeSelection,
                new LiveEvaluationStreamService(),
                new EngineLineDisplayService());
        ComputerMoveService computerMoveService =
                new ComputerMoveService(gameService, runtimeSelection);
        GameLifecycleService lifecycleService =
                new GameLifecycleService(gameService, computerMoveService, evaluationService);

        GameSettingsDto settings = assertDoesNotThrow(() ->
                lifecycleService.startNewGame(new GameSettingsDto(
                        300,
                        0,
                        0,
                        0,
                        "WHITE",
                        0)));

        assertTrue(settings.getVersion() > 0);
    }

    private void assertUnavailable(
            Role expectedRole,
            org.junit.jupiter.api.function.Executable operation) {
        NativeEngineUnavailableException exception =
                assertThrows(NativeEngineUnavailableException.class, operation);
        assertEquals(expectedRole, exception.getRole());
    }

    private EngineSettingsService getSettingsService(
            EngineRuntimeSelectionService runtimeSelection) throws Exception {
        java.lang.reflect.Field field =
                EngineRuntimeSelectionService.class.getDeclaredField("engineSettingsService");
        field.setAccessible(true);
        return (EngineSettingsService) field.get(runtimeSelection);
    }

    private EngineRuntimeSelectionService createRuntimeSelectionWithoutEngine() throws Exception {
        Path games = Files.createDirectories(tempDir.resolve("games"));
        configureProperties(games);

        EngineSettingsService settingsService = new EngineSettingsService(
                new ObjectMapper(),
                new EngineDiscoveryService());

        return new EngineRuntimeSelectionService(settingsService);
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
}
