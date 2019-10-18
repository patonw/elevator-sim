package elevator.simulation;

import elevator.event.RunnableEventBus;
import elevator.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public abstract class AbstractSimulator {
    protected final RunnableEventBus bus;
    private AtomicLong clock = new AtomicLong(1);

    public AbstractSimulator(RunnableEventBus bus) {
        this.bus = bus;
    }

    @NotNull
    public Stream<Event.ClockTick> getClockStream() {
        return LongStream.generate(clock::incrementAndGet).mapToObj(Event.ClockTick::new);
    }

    abstract void start() throws InterruptedException;
}
