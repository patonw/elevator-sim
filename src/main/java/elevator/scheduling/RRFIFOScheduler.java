package elevator.scheduling;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventTopic;
import elevator.model.Elevator;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A predictably unsophisticated scheduler that isn't completely awful.
 */
public class RRFIFOScheduler implements Scheduler {
    private Elevator[] elevators;
    private AtomicInteger ctr = new AtomicInteger(0);

    @Override
    public RRFIFOScheduler setElevators(Elevator[] elevators) {
        this.elevators = elevators;
        return this;
    }

    @Override
    public void onEvent(EventBus bus, Event event) {
        if (event instanceof Event.ScheduleRequest) {
            handleScheduleRequest(bus, (Event.ScheduleRequest) event);
        }
    }

    private void handleScheduleRequest(EventBus bus, Event.ScheduleRequest event) {
        int assignee = ctr.getAndIncrement() % elevators.length;

        bus.fireTopic(EventTopic.SCHEDULING, new Event.AssignRequest(event.getPassenger(), event.getStart(), assignee));
    }
}
