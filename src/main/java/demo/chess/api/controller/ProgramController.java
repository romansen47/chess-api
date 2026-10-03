package demo.chess.api.controller;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import demo.chess.api.dto.ProgramFeaturesDto;
import demo.chess.api.service.LiveEvaluationStreamService;
import demo.chess.api.service.ProgramFeatureService;
import demo.chess.definitions.engines.management.UciEngineProcessInfo;
import demo.chess.definitions.engines.management.UciEngineProcessManager;

@RestController
@RequestMapping("/api/program")
public class ProgramController {

    private static final String TERMINATE_HEADER = "X-Chess-Terminate";
    private static final String TERMINATE_HEADER_VALUE = "terminate";
    private static final long SHUTDOWN_DELAY_MILLIS = 500L;

    private final ProgramFeatureService programFeatureService;
    private final LiveEvaluationStreamService liveEvaluationStreamService;
    private final AtomicBoolean terminationScheduled =
            new AtomicBoolean(false);

    /**
     * Creates a new ProgramController instance.
     *
     * @param programFeatureService runtime feature switches
     * @param liveEvaluationStreamService browser live-evaluation SSE lifecycle
     */
    public ProgramController(
            ProgramFeatureService programFeatureService,
            LiveEvaluationStreamService liveEvaluationStreamService) {
        this.programFeatureService = programFeatureService;
        this.liveEvaluationStreamService = liveEvaluationStreamService;
    }

    /**
     * Returns runtime feature switches that affect the frontend.
     *
     * @return current program features
     */
    @GetMapping("/features")
    public ProgramFeaturesDto getProgramFeatures() {
        return new ProgramFeaturesDto(
                programFeatureService.isDebugModeEnabled());
    }

    /**
     * Performs the terminate program operation.
     *
     * @param terminateHeader the terminate header
     * @return the result of the operation
     */
    @PostMapping("/terminate")
    public ResponseEntity<Map<String, Object>> terminateProgram(
            @RequestHeader(
                    name = TERMINATE_HEADER,
                    required = false)
                    String terminateHeader) {
        if (!TERMINATE_HEADER_VALUE.equals(terminateHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "accepted", false,
                            "message",
                            "Missing or invalid termination header"));
        }

        boolean newlyScheduled =
                terminationScheduled.compareAndSet(false, true);
        if (newlyScheduled) {
            Thread shutdownThread = new Thread(
                    this::shutdownApplication,
                    "chess-program-shutdown");
            shutdownThread.setDaemon(false);
            shutdownThread.start();
        }

        return ResponseEntity.accepted()
                .body(Map.of(
                        "accepted", true,
                        "alreadyScheduled",
                        !newlyScheduled));
    }

    /**
     * Performs the shutdown application operation.
     */
    private void shutdownApplication() {
        try {
            Thread.sleep(SHUTDOWN_DELAY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        liveEvaluationStreamService.closeAllSubscribers();

        for (UciEngineProcessInfo processInfo
                : UciEngineProcessManager.list()) {
            if (!processInfo.processAlive()) {
                continue;
            }

            try {
                UciEngineProcessManager.terminate(processInfo.id());
            } catch (RuntimeException ignored) {
                // Continue shutdown even if one external UCI process resists
                // termination.
            }
        }

        /*
         * SpringApplication already registered the JVM shutdown hook that
         * closes the ApplicationContext. Trigger that single canonical path
         * instead of closing the context manually and then starting JVM
         * shutdown a second time.
         */
        System.exit(0);
    }
}
