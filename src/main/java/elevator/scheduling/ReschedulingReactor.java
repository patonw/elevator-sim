package elevator.scheduling;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventReactor;
import elevator.event.EventTopic;
import elevator.model.Passenger;

public class ReschedulingReactor implements EventReactor {
    @Override
    public void onEvent(EventBus bus, Event event) {
        if (event instanceof Event.MissedConnection) {
            final Event.MissedConnection missed = (Event.MissedConnection) event;
            final Passenger passenger = missed.getPassenger();
            final int floor = missed.getFloor();

            bus.fire(EventTopic.SCHEDULING, new Event.ScheduleRequest(passenger, floor));
        }
    }

    @Override
    public void syncEvent(EventBus bus, Event event) {
        onEvent(bus, event);
    }
}
