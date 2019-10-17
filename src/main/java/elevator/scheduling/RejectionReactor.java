package elevator.scheduling;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventReactor;
import elevator.event.EventTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retries rejected requests indefinitely.
 *
 */
public class RejectionReactor implements EventReactor {
    private static final Logger log = LoggerFactory.getLogger(RejectionReactor.class);

    @Override
    public void onEvent(EventBus bus, Event event) {
        if (!(event instanceof Event.RequestRejected))
            return;

        Event.AssignRequest request = ((Event.RequestRejected) event).getRequest();
        log.warn("Assignment rejected! " + request + "... retrying.");

        // TODO Implement retry limit. Will require monitoring RequestAccepted too.
        bus.fire(EventTopic.SCHEDULING, new Event.ScheduleRequest(request.getPassenger(), request.getFloor()));
    }
}
