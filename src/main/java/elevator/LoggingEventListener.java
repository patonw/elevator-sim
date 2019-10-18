package elevator;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventReactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingEventListener implements EventReactor {
    private static Logger singleton = LoggerFactory.getLogger(LoggingEventListener.class);
    private Logger log = LoggerFactory.getLogger(LoggingEventListener.class);
    private long simTime = 0;

    public LoggingEventListener(Logger log) {
        this.log = log;
    }

    public LoggingEventListener() {
        this(singleton);
    }

    @Override
    public void syncEvent(EventBus bus, Event event) {
        onEvent(bus, event);
    }

    @Override
    public void onEvent(EventBus bus, Event event) {
        if (event instanceof Event.ClockTick) {
            simTime = ((Event.ClockTick) event).getValue();
        }

        log.info(String.format("{T=%04d} %s", simTime, event));
    }
}
