package elevator.event;

import java.util.EnumSet;

public interface EventBus {
    void attach(EnumSet<EventTopic> topics, EventReactor listener);
    
    default void attach(EventReactor listener) {
        attach(EnumSet.allOf(EventTopic.class), listener);
    }

    void fire(EventTopic topic, Event event);

    default void fire(Event event) {
        fire(EventTopic.DEFAULT, event);
    }
}
