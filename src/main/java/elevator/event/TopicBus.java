package elevator.event;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is a partition of the main bus that routes messages for a particular topic locally
 * but sends messages for other topics up to the parent to route.
 */
public class TopicBus implements RunnableEventBus {
    private static final Logger log = LoggerFactory.getLogger(TopicBus.class);
    private final EventTopic topic;
    private final EventBus parent;

    private AtomicReference<Set<EventReactor>> reactors = new AtomicReference<>(HashSet.empty());
    private BlockingQueue<Event> queue;

    public TopicBus(EventTopic topic, EventBus parent, int queueDepth) {
        this.topic = topic;
        this.parent = parent;
        this.queue = new LinkedBlockingQueue<>(queueDepth);
    }

    /**
     * Users should not call this directly.
     * The parent bus will delegate attachers to this bus when the topic matches.
     *
     * @param topics Set of topics to subscribe to
     * @param reactor The object that will be notified of incoming events
     */
    @Override
    public void attach(EnumSet<EventTopic> topics, EventReactor reactor) {
        assert(topics.contains(topic));

        while (true) {
            final Set<EventReactor> oldValue = reactors.get();
            final Set<EventReactor> newValue = oldValue.add(reactor);

            if (reactors.compareAndSet(oldValue, newValue))
                break;
        }

    }

    @Override
    public void fire(EventTopic topic, Event event) {
        if (this.topic.equals(topic)) {
            try {
                queue.put(event);
            } catch (InterruptedException e) {
                log.warn("Interrupted while firing event", e);
            }
        }
        else {
            parent.fire(topic, event);
        }
    }

    private void dispatch(Event event) {
        reactors.get()
                .toStream().shuffle()
                .forEach(handler -> {
                    handler.syncEvent(this, event);
                });
    }

    @Override
    public int process(int limit) {
        int ctr = 0;
        for (int i=0; i<limit; i++) {
            Event event = queue.poll();
            if (event == null)
                break;

            dispatch(event);
        }

        return ctr;
    }

    @Override
    public void run(AtomicBoolean shutdownFlag) throws InterruptedException {
        while (true) {
            Event event = queue.poll(100, TimeUnit.MILLISECONDS);
            if (event != null)
                dispatch(event);

            if (shutdownFlag.get()) {
                return;
            }
        }
    }

    @Override
    public long getBacklog() {
        return queue.size();
    }
}
