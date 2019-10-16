package elevator.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

// TODO make this an EventReactor and attach it to the bus instead
// When receives clock tick, it fires off events for the current cycle

/**
 * Defer events to fire at predetermined clock ticks.
 *
 * Used for offline simulations (e.g. integration tests) and scheduling health checks.
 */
public class DeferredEventQueue implements EventReactor {
    private static final Logger log = LoggerFactory.getLogger(DeferredEventQueue.class);
    private AtomicLong clock = new AtomicLong(0);
    private Set<EventReactor> allTopics = new HashSet<>();
    private PriorityQueue<DeferredEvent> events = new PriorityQueue<>(
            Comparator.comparing(DeferredEvent::getTime)
                    .thenComparing(DeferredEvent::getId));

//    public void attach(EnumSet<EventTopic> topics, EventReactor listener) {
//        // TODO implement topics
//        allTopics.add(listener);
//    }

    public long getClock() {
        return this.clock.get();
    }

    public int getNumListeners() {
        return allTopics.size();
    }

    public void scheduleAt(long time, Event event) {
        scheduleAt(time, EventTopic.DEFAULT, event);
    }

    public void scheduleAt(long time, EventTopic topic, Event event) {
        if (time < getClock())
            throw new IllegalArgumentException("Cannot schedule an event in the past");

        events.add(new DeferredEvent(time, topic, event));
    }
//
//    public void fire(EventTopic topic, Event event) {
//        scheduleAt(getClock(), topic, event);
//    }

    public boolean isActive() {
        return !events.isEmpty();
    }

//    // TODO concurrency
//    private void processCurrent() {
//        while (!events.isEmpty() && (events.peek().getTime() <= getClock())) {
//            DeferredEvent deferred = events.poll();
//            dispatch(deferred.getTopic(), deferred.getEvent());
//        }
//    }
//
//    /**
//     * Perform a clock tick in an offline simulation.
//     *
//     * Dispatches all events scheduled for the current clock cycle, then increments the clock.
//     * While some events are queued before simulation starts, during the run other events will be
//     * fired from listeners handling currently scheduled events.
//     *
//     * @return The new clock value
//     */
//    private long step() {
//        processCurrent();
//        long tick = clock.incrementAndGet();
//        dispatch(EventTopic.DEFAULT, new Event.ClockTick(tick));
//
//        return tick;
//    }
//
//    // TODO move into own OfflineSimulator class
//    public void simulate(long stepTime) throws InterruptedException {
//        final long start = System.currentTimeMillis();
//        while (isActive()) {
//            long target = start + stepTime * getClock();
//            long delta = target - System.currentTimeMillis();
//            log.debug("Stepping to clock cycle #{}. Sleeping for {}ms", getClock(), delta);
//
//            if (delta > 0)
//                Thread.sleep(delta);
//
//            step();
//        }
//
//        log.debug("Simulation completed at clock cycle #{}", getClock());
//    }
//
//    public void simulate() {
//        while (isActive())
//            step();
//    }
//
//    private void dispatch(EventTopic topic, Event event) {
//        // TODO route event only to relevant topic listeners
//        allTopics.forEach(handler -> {
//           handler.onEvent(this, event);
//        });
//    }

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
