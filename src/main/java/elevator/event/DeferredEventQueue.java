package elevator.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Defer events to fire at predetermined clock ticks.
 *
 * Used for offline simulations (e.g. integration tests) and scheduling health checks.
 */
public class DeferredEventQueue implements EventReactor {
    private static final Logger log = LoggerFactory.getLogger(DeferredEventQueue.class);
    private AtomicLong clock = new AtomicLong(0);
    private PriorityQueue<DeferredEvent> events = new PriorityQueue<>(
            Comparator.comparing(DeferredEvent::getTime)
                    .thenComparing(DeferredEvent::getId));

    public long getClock() {
        return this.clock.get();
    }

    public void scheduleAt(long time, Event event) {
        scheduleAt(time, EventTopic.DEFAULT, event);
    }

    public void scheduleAt(long time, EventTopic topic, Event event) {
        if (time < getClock())
            throw new IllegalArgumentException("Cannot schedule an event in the past");

        events.add(new DeferredEvent(time, topic, event));
    }

    public boolean isActive() {
        return !events.isEmpty();
    }

    @Override
    public void onEvent(EventBus bus, Event event) {
        if (event instanceof Event.ClockTick) {
            Event.ClockTick tick = (Event.ClockTick) event;
            clock.set(tick.getValue());

            while (!events.isEmpty() && (events.peek().getTime() <= tick.getValue())) {
                final DeferredEvent deferred = events.remove();
                bus.fire(deferred.getTopic(), deferred.getEvent());
            }
        }
    }

    protected static class DeferredEvent {
        private static AtomicLong ctr = new AtomicLong(0);
        private long id;
        private long time;
        private EventTopic topic;
        private Event event;

        DeferredEvent(long time, EventTopic topic, Event event) {
            this.id = ctr.getAndIncrement();
            this.time = time;
            this.topic = topic;
            this.event = event;
        }

        long getId() {
            return id;
        }

        long getTime() {
            return time;
        }

        EventTopic getTopic() {
            return topic;
        }

        Event getEvent() {
            return event;
        }
    }
}
