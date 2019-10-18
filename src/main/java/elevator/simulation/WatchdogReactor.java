package elevator.simulation;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventReactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class WatchdogReactor implements EventReactor {
    private static final Logger log = LoggerFactory.getLogger(WatchdogReactor.class);
    private AtomicLong clock = new AtomicLong(0);

    @Override
    public void syncEvent(EventBus bus, Event event) {
        onEvent(bus, event);
    }

    @Override
    public void onEvent(EventBus bus, Event event) {
        // TODO monitor rate of clock ticks
        // TODO check queue depth
        // TODO initiate recovery action
        if (event instanceof Event.ClockTick) {
            clock.set(((Event.ClockTick) event).getValue());
        } else if (event instanceof Event.ElevatorArrived) {
            final long evtTime = ((Event.ElevatorArrived) event).getClock();
            if (Math.abs(evtTime - clock.get()) > 1) {
                log.error("EVENT BUS IS SATURATED!!! Falling behind!!!");
            }

        }
    }
}