package elevator.scheduling;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventReactor;
import elevator.event.EventTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Retries rejected requests immediately, indefinitely.
 */
public class RejectionReactor implements EventReactor {
    private static final Logger log = LoggerFactory.getLogger(RejectionReactor.class);
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
    private final int jitter;

    public RejectionReactor() {
        this(10);
    }

    public RejectionReactor(int jitter) {
        this.jitter = jitter;
    }

    @Override
    public void onEvent(EventBus bus, Event event) {
        if (!(event instanceof Event.RequestRejected))
            return;

        Event.AssignRequest request = ((Event.RequestRejected) event).getRequest();

        executor.schedule(() -> {
            bus.fire(EventTopic.SCHEDULING, new Event.ScheduleRequest(request.getPassenger(), request.getFloor()));
        }, ThreadLocalRandom.current().nextLong(jitter), TimeUnit.MILLISECONDS);
    }

    @Override
    public void syncEvent(EventBus bus, Event event) {
        onEvent(bus, event);
    }
}
