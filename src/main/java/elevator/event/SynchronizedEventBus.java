package elevator.event;

import elevator.simulation.DeferredEventQueue;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

// Could use quite a bit of optimization but good enough for a proof of concept
public class SynchronizedEventBus implements RunnableEventBus {
    private static final Logger log = LoggerFactory.getLogger(DeferredEventQueue.class);

    // tempted to use functional collections with atomic references instead
    private Set<EventReactor> allTopics = Collections.synchronizedSet(new HashSet<>());
    private BlockingQueue<Tuple2<EventTopic, Event>> queue = new LinkedBlockingQueue<>();

    @Override
    public void attach(EnumSet<EventTopic> topics, EventReactor listener) {
        // TODO implement topics
        allTopics.add(listener);
    }

    public int getNumListeners() {
        return allTopics.size();
    }

    /**
     * Enqueue a new event to be processed later or by a concurrent worker.
     *
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
        allTopics.forEach(handler -> {
            handler.syncEvent(this, event);
        });
    }

    @Override
    public int process() {
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
    public void run(Semaphore shutdownSig) throws InterruptedException {
        while (true) {
            Tuple2<EventTopic, Event> event = queue.take();
            dispatch(event._1, event._2);

            // Could just use an atomic boolean instead
            if (shutdownSig.tryAcquire())
                return;
        }
    }
}
