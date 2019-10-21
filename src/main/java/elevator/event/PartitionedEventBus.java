package elevator.event;

import io.vavr.Tuple;
import io.vavr.collection.Array;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PartitionedEventBus implements RunnableEventBus {
    private static final Logger log = LoggerFactory.getLogger(SynchronizedEventBus.class);
    EnumMap<EventTopic, TopicBus> topicBus = new EnumMap<>(EventTopic.class);
    EnumMap<EventTopic, Integer> topicWorkers = new EnumMap<>(EventTopic.class);
    EnumMap<EventTopic, Integer> topicPriority = new EnumMap<>(EventTopic.class);
    private ExecutorService executors = Executors.newCachedThreadPool(); // TODO configurable

    public PartitionedEventBus(int queueDepth) {
        Arrays.stream(EventTopic.values()).forEach(topic -> {
            topicBus.put(topic, new TopicBus(topic, this, queueDepth));
        });
    }

    public PartitionedEventBus() {
        this(1024);
    }

    public PartitionedEventBus setTopicWorkers(EventTopic topic, int numWorkers) {
        topicWorkers.put(topic, numWorkers);
        return this;
    }

    public PartitionedEventBus setTopicPriority(EventTopic topic, int priority) {
        topicPriority.put(topic, priority);
        return this;
    }

    @Override
    public Health health() {
        return Stream.ofAll(topicBus.values())
                .map(TopicBus::health)
                .map(Enum::ordinal)
                .max()
                .map(x -> EventBus.Health.cardinal[x])
                .getOrElse(Health.DEGRADED);
    }

    @Override
    public int process(int limit) {
        AtomicInteger ctr = new AtomicInteger(0);
        topicBus.forEach((topic, bus) -> {
            ctr.addAndGet(bus.process(limit));
        });

        return ctr.get();
    }

    public void dynamicRun(AtomicBoolean shutdownFlag) throws InterruptedException {
        final Array<EventTopic> topics = Array.of(EventTopic.values());
        // TODO configurable

        CountDownLatch childLatch = new CountDownLatch(topics.length());
        topics.map(topic -> {
            int numWorkers = topicWorkers.getOrDefault(topic, 1);
            TopicBus bus = topicBus.get(topic);
            final Integer priority = topicPriority.getOrDefault(topic, Thread.NORM_PRIORITY);

            var ths = new Thread(() ->
                    Try.run(() -> {
                        bus.dynamicRun(shutdownFlag, executors, numWorkers);
                    }).onFailure(ex -> {
                        log.error("Topic thread died. Shutting down", ex);
                        shutdownFlag.set(true);
                    }).andFinally(childLatch::countDown));
            ths.setName(String.format("%-6.6s", topic.name()));
            ths.setPriority(priority);
            ths.start();
            return ths;
        });
        childLatch.await();
    }

    @Override
    public void run(AtomicBoolean shutdownFlag) throws InterruptedException {
        final Array<EventTopic> topics = Array.of(EventTopic.values());
        final int totalWorkers = topics.map(t -> topicWorkers.getOrDefault(t, 1)).sum().intValue();

        log.info("Starting a total of {} workers", totalWorkers);
        CountDownLatch childLatch = new CountDownLatch(totalWorkers);

        final Array<Thread> threads = topics.flatMap(topic -> {
            int n = topicWorkers.getOrDefault(topic, 1);
            TopicBus bus = topicBus.get(topic);
            final Integer priority = topicPriority.getOrDefault(topic, Thread.NORM_PRIORITY);

            final Stream<Thread> ths = Stream.range(0, n).map(i ->
                    new Thread(() ->
                            Try.run(() -> {
                                bus.run(shutdownFlag);
                            }).onFailure(ex -> {
                                log.error("Topic thread died. Shutting down", ex);
                                shutdownFlag.set(true);
                            }).andFinally(childLatch::countDown)));

            ths.zipWithIndex()
                    .map(pair -> Tuple.of(pair._1, String.format("%-6.6s-%02d", topic.name(), pair._2)))
                    .forEach(pair -> pair._1.setName(pair._2));
            ths.forEach(th -> th.setPriority(priority));
            ths.forEach((Thread::start));

            return ths;
        });


        // TODO check for startup exceptions from futures

        childLatch.await();
    }

    @Override
    public void attach(EnumSet<EventTopic> topics, EventReactor reactor) {
        topics.forEach(topic -> {
            topicBus.get(topic).attach(topics, reactor);
        });
    }

    @Override
    public void fire(EventTopic topic, Event event) {
        topicBus.get(topic).fire(topic, event);
    }

    @Override
    public long getBacklog() {
        return Stream.ofAll(topicBus.values()).map(TopicBus::getBacklog).sum().longValue();
    }
}
