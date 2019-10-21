package elevator.simulation;


import elevator.event.Event;
import elevator.event.EventTopic;
import elevator.event.PartitionedEventBus;
import elevator.event.RunnableEventBus;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class FixedRateSimulator extends AbstractSimulator {
    private static final Logger log = LoggerFactory.getLogger(FixedRateSimulator.class);
    private long rate = 1000;
    final int workers;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private final AtomicBoolean shutdownFlag = new AtomicBoolean(false);
    private AtomicLong clock = new AtomicLong(1);

    public FixedRateSimulator(RunnableEventBus bus, long rate, int workers) {
        super(bus);
        this.rate = rate;
        this.workers = workers;
    }

    public FixedRateSimulator(RunnableEventBus bus, long rate) {
        this(bus, rate, 1);
    }

    public long getRate() {
        return rate;
    }

    public void setRate(long rate) {
        this.rate = rate;
    }

    public void shutdown() {
        log.info("Shutdown signal sent");
        shutdownFlag.set(true);
        Thread.yield();
    }

    /**
     * Runs the event loop in a new thread (from the default pool).
     *
     * The future returned can be used to monitor the state of the loop and to join
     * it to the main thread. However, use {@link #shutdown()} to gracefully terminate
     * the simulation.
     *
     * @return A future that can be used to join the task to the main thread.
     */
    public CompletableFuture<Try<Void>> startAsync() {
        return CompletableFuture.supplyAsync(() -> Try.run(this::start));
    }

    /**
     * Runs the event loop blocking the current thread.
     *
     * @throws InterruptedException
     */
    @Override
    public void start() throws InterruptedException {
        Future<?> scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
            bus.fire(EventTopic.DEFAULT, new Event.ClockTick(clock.getAndIncrement()));
        }, 100L, rate, TimeUnit.MILLISECONDS);

        if (bus instanceof PartitionedEventBus)
            ((PartitionedEventBus) bus).dynamicRun(shutdownFlag);
        else
            bus.run(shutdownFlag);

        log.info("Shutting down simulation: {}", shutdownFlag);
        scheduledFuture.cancel(false);

        try {
            scheduledFuture.get();
        } catch (ExecutionException e) {
            log.warn("Error while cancelling simulation", e);
        }
    }
}
