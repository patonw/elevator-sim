package elevator.event;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// Could use quite a bit of optimization but good enough for a proof of concept
public class SynchronizedEventBus implements RunnableEventBus {
    private static final Logger log = LoggerFactory.getLogger(SynchronizedEventBus.class);

    private AtomicReference<Set<EventReactor>> allTopics = new AtomicReference<>(HashSet.empty());
    private BlockingQueue<Tuple2<EventTopic, Event>> queue = new LinkedBlockingQueue<>();


    @Override
    public void attach(EnumSet<EventTopic> topics, EventReactor listener) {
        // TODO implement topics
        while (true) {
            final Set<EventReactor> oldValue = allTopics.get();
            final Set<EventReactor> newValue = oldValue.add(listener);

            if (allTopics.compareAndSet(oldValue, newValue))
                break;
        }
    }

    public long getBacklog() {
        return queue.size();
    }

    public int getNumListeners() {
        return allTopics.get().size();
    }

    /**
     * Enqueue a new event to be processed later or by a concurrent worker.
     * <p>
     * Queue is unbounded so this should not block during normal operation.
     *
     * @param topic
     * @param event
     */
    @Override
    public void fire(EventTopic topic, Event event) {
        try {
            queue.put(Tuple.of(topic, event));
        } catch (InterruptedException e) {
            log.warn("Interrupted while firing event", e);
        }
    }

    private void dispatch(EventTopic topic, Event event) {
        // TODO route event only to relevant topic listeners
        allTopics.get()
                .toStream().shuffle()
                .forEach(handler -> {
                    handler.syncEvent(this, event);
                });
    }

    @Override
    public int process(int limit) {
        int ctr = 0;
        while (true) {
            Tuple2<EventTopic, Event> event = queue.poll();
            if (event == null)
                break;

            dispatch(event._1, event._2);
        }

        return ctr;
    }

    @Override
    public void run(AtomicBoolean shutdownFlag) throws InterruptedException {
        while (true) {
            Tuple2<EventTopic, Event> event = queue.take();
            dispatch(event._1, event._2);

            if (shutdownFlag.get()) {
                return;
            }
        }
    }
}
