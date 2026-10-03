package demo.chess.api.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LiveEvaluationStreamServiceTest {

    @Test
    void closeAllSubscribersReleasesOpenSseRequests() {
        LiveEvaluationStreamService service =
                new LiveEvaluationStreamService();

        service.subscribe();
        service.subscribe();

        assertTrue(service.hasSubscribers());

        service.closeAllSubscribers();

        assertFalse(service.hasSubscribers());
    }

    @Test
    void closeAllSubscribersIsIdempotent() {
        LiveEvaluationStreamService service =
                new LiveEvaluationStreamService();

        service.subscribe();
        service.closeAllSubscribers();
        service.closeAllSubscribers();

        assertFalse(service.hasSubscribers());
    }
}
