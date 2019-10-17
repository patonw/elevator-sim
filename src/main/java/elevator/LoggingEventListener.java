package elevator;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventReactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingEventListener implements EventReactor {
    private static Logger singleton = LoggerFactory.getLogger(LoggingEventListener.class);
    private Logger log = LoggerFactory.getLogger(LoggingEventListener.class);

    public LoggingEventListener(Logger log) {
        this.log = log;
    }

    public LoggingEventListener() {
        this(singleton);
    }

    @Override
    public void onEvent(EventBus bus, Event event) {
        log.info("{" + event.getClass().getSimpleName() + "} " + event);
    }
}
