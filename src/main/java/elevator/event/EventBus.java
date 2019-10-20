package elevator.event;

import java.util.EnumSet;

public interface EventBus {
    enum Health {
        GOOD,
        DEGRADED,
        CRITICAL;

        public static final Health[] cardinal = values();
    }

    default Health health() {
        return Health.GOOD;
    }

    /**
     * Attaches a reactor to the event bus such that any events on the subscribed
     * topic will be delivered to the reactor.
     *
     * @param topics Set of topics to subscribe to
     * @param reactor The object that will be notified of incoming events
     */
    void attach(EnumSet<EventTopic> topics, EventReactor reactor);
    
    default void attach(EventReactor listener) {
        attach(EnumSet.allOf(EventTopic.class), listener);
    }

    /**
     * Used by reactors to publish events to the bus.
     *
     * Each event can only have one topic.
     * However, a single reactor can subscribe to multiple topics.
     *
     * @param topic The topic of the message.
     * @param event The event to broadcast
     */
    void fire(EventTopic topic, Event event);

    default void fire(Event event) {
        fire(EventTopic.DEFAULT, event);
    }
}
