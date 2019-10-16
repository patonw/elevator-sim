package elevator.simulation;

import elevator.event.SynchronizedEventBus;

/**
 * Offline simulation without delay for integration tests
 *
 */
public class OfflineSimulator extends AbstractSimulator {
    private long limit = 0;
    public OfflineSimulator(SynchronizedEventBus bus) {
        super(bus);
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public void runTo(long limit) {
        setLimit(limit);
        start();
    }

    @Override
    void start() {
        getClockStream()
                .takeWhile(tick -> tick.getValue() <= limit)
                .forEach(tick -> {
                    bus.fire(tick);
                    bus.process();
                });
    }
}
