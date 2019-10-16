package elevator.simulation;


import elevator.event.Event;
import elevator.event.SynchronizedEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class FixedRateSimulator extends AbstractSimulator {
    private static final Logger log = LoggerFactory.getLogger(FixedRateSimulator.class);
    private long rate = 1000;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private final Semaphore shutdownSig = new Semaphore(0);
    private AtomicLong clock = new AtomicLong(0);

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
        scheduler.scheduleAtFixedRate(() -> {
            bus.fire(new Event.ClockTick(clock.getAndIncrement()));
            bus.process();
        }, 0L, rate, TimeUnit.MILLISECONDS);

        shutdownSig.acquire();
    }
}
