package elevator.event;

public interface EventReactor {
    /**
     * Reacts to an published event.
     *
     * Can create and send additional events in reaction to the incoming one.
     *
     * @param bus The event bus to send reactions
     * @param event The incoming event
     */
    void onEvent(EventBus bus, Event event);

    /**
     * Handles an event while synchronized on the reactor.
     *
     * This only provides concurrency protection as long as reactors do not share mutable state.
     * This is fairly coarse-grained and not suitable for high throughput, but can be overridden
     * by reactors with more intelligent behaviour (e.g. ones using lock-free queuing, etc.).
     *
     * @param bus The main event bus to publish replies to
     * @param event The event to be processed
     */
    default void syncEvent(EventBus bus, Event event) {
        synchronized(this) {
            onEvent(bus, event);
        }
    }
}
