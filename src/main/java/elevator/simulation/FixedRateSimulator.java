package elevator.simulation;


import elevator.event.Event;
import elevator.event.EventTopic;
import elevator.event.SynchronizedEventBus;
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

    public FixedRateSimulator(SynchronizedEventBus bus, long rate) {
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

    @Override
    public void start() throws InterruptedException {
        ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
            bus.fire(EventTopic.DEFAULT, new Event.ClockTick(clock.getAndIncrement()));
            bus.process();
        }, 0L, rate, TimeUnit.MILLISECONDS);

        // Block until shutdown signal
        shutdownSig.acquire();

        log.info("Shutting down simulation");
        scheduledFuture.cancel(false);

        try {
            scheduledFuture.get();
        } catch (ExecutionException e) {
            log.warn("Error while cancelling simulation", e);
        }
    }
}
