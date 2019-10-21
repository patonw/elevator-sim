package elevator.event;

import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class OverloadStrategy {
    private static final Logger log = LoggerFactory.getLogger(OverloadStrategy.class);
    private static final int workLimit = 4096;
    private static final int pauseLimit = 128;

    private AtomicInteger concurrentWorkers = new AtomicInteger(0);
    private AtomicInteger workerId = new AtomicInteger(0);

    private ExecutorService executor;
    private int maxWorkers;

    public OverloadStrategy(ExecutorService executor, int maxWorkers) {
        this.executor = executor;
        this.maxWorkers = maxWorkers;
    }

    public void apply(TopicBus topicBus) {
        if (topicBus.health() != EventBus.Health.GOOD)
            if (concurrentWorkers.get() < maxWorkers)
                spawn(topicBus);
    }

    public void spawn(TopicBus topicBus) {
        EventTopic topic = topicBus.getTopic();

        final int me = workerId.incrementAndGet() % 100;
        final int sleepTime = me % maxWorkers;
        concurrentWorkers.getAndIncrement();

        log.info("Topic bus for {} spawning worker #{} ({}/{}) to handle load", topic, me, concurrentWorkers.get(), maxWorkers);
        executor.submit(() -> {
            Thread.currentThread().setName(String.format("%-6.6s-%02d", topic.name(), me));
            int handled = 0;
            int pauses = 0;

            while (handled < workLimit && pauses < pauseLimit) {
                int last = topicBus.process(workLimit);

                if (last > 0)
                    pauses = 0;

                handled += last;
                ++pauses;

                if (Try.run(() -> Thread.sleep(sleepTime)).isFailure())
                    break;
            }

            log.info("Worker #{} ({}/{}) for {} done processing {} events", me, concurrentWorkers.get(), maxWorkers, topic, handled);
            concurrentWorkers.decrementAndGet();
        });
    }
}
