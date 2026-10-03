package demo.chess.api.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Keeps the browser-facing SSE connections for the normal live-evaluation bar.
 * Engine threads never send to these emitters directly; callers publish from
 * a separate worker so slow clients cannot block UCI output processing.
 */
@Service
public class LiveEvaluationStreamService {

    private static final Log logger = LogFactory.getLog(LiveEvaluationStreamService.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Creates and registers a live-evaluation SSE connection.
     * @return the emitter
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        Runnable remove = () -> emitters.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(() -> {
            remove.run();
            emitter.complete();
        });
        emitter.onError(error -> remove.run());

        try {
            emitter.send(SseEmitter.event().name("ready").data("ready"));
        } catch (IOException e) {
            remove.run();
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Returns whether at least one browser currently listens for live updates.
     * @return true when a subscriber exists
     */
    public boolean hasSubscribers() {
        return !emitters.isEmpty();
    }

    /**
     * Completes every open browser SSE request.
     *
     * <p>Long-lived SSE requests otherwise remain active while Tomcat starts
     * graceful shutdown and can keep the web-server lifecycle phase alive
     * until its timeout expires.</p>
     */
    public void closeAllSubscribers() {
        List<SseEmitter> subscribers = List.copyOf(emitters);
        emitters.clear();

        for (SseEmitter emitter : subscribers) {
            try {
                emitter.complete();
            } catch (RuntimeException e) {
                logger.debug(
                        "Could not complete live-evaluation SSE client during shutdown: "
                                + e.getMessage());
            }
        }
    }

    /**
     * Ensures non-UI shutdown paths such as SIGTERM also release SSE requests
     * before the web server enters its graceful-shutdown lifecycle phase.
     *
     * @param event context-close event
     */
    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        closeAllSubscribers();
    }

    /**
     * Publishes one selected bar update to all current subscribers.
     * @param evaluation evaluation in pawns
     * @param bar normalized white share of the bar
     * @param depth search depth
     */
    public void publish(double evaluation, double bar, int depth) {
        Map<String, Object> payload = Map.of(
                "eval", evaluation,
                "bar", bar,
                "depth", depth);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(Integer.toString(depth))
                        .name("bar")
                        .data(payload));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // The connection is already gone.
                }
                logger.debug("Removed closed live-evaluation SSE client: " + e.getMessage());
            }
        }
    }
}
