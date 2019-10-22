package elevator;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventReactor;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;

public class ChronicleAppenderListener implements EventReactor {
    private final ChronicleQueue queue;
    private final ExcerptAppender appender;
    private final EventBus eventBus;

    public ChronicleAppenderListener(String dir) {
        queue = ChronicleQueue.singleBuilder(dir).build();
        appender = queue.acquireAppender();
        eventBus = appender.methodWriter(EventBus.class);
    }

    @Override
    public void onEvent(EventBus bus, Event event) {
        eventBus.fire(event);
    }
}
