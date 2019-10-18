package elevator.simulation;


import elevator.event.Event;
import elevator.event.EventTopic;
import elevator.event.RunnableEventBus;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class FixedRateSimulator extends AbstractSimulator {
    private static final Logger log = LoggerFactory.getLogger(FixedRateSimulator.class);
    private long rate = 1000;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private final Semaphore shutdownSig = new Semaphore(0);
    private AtomicLong clock = new AtomicLong(1);

    public FixedRateSimulator(RunnableEventBus bus, long rate) {
        super(bus);
        this.rate = rate;
    }

    public long getRate() {
        return rate;
    }

    public void setRate(long rate) {
        this.rate = rate;
    }

    public void shutdown() {
        shutdownSig.release();
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
    public Future<Void> startAsync() {
        return CompletableFuture.runAsync(() -> Try.run(this::start));
    }

    /**
     * Runs the event loop blocking the current thread.
     *
     * @throws InterruptedException
     */
    @Override
    public void start() throws InterruptedException {
        ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
            bus.fire(EventTopic.DEFAULT, new Event.ClockTick(clock.getAndIncrement()));
        }, 100L, rate, TimeUnit.MILLISECONDS);

        // Blocks until shutdown signal is given
        bus.run(shutdownSig);

        log.info("Shutting down simulation");
        scheduledFuture.cancel(false);

        try {
            scheduledFuture.get();
        } catch (ExecutionException e) {
            log.warn("Error while cancelling simulation", e);
        }
    }
}
