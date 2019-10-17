package elevator;

import elevator.event.*;
import elevator.simulation.FixedRateSimulator;
import elevator.simulation.OfflineSimulator;
import io.vavr.control.Try;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.sql.Time;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeast;

// Ensure that the FixedRateSimulator fires clock ticks at a specified rate
public class FixedRateSimulatorTest {
    @Test
    public void testSimulation() throws InterruptedException, TimeoutException, ExecutionException {
        SynchronizedEventBus bus = new SynchronizedEventBus();
        FixedRateSimulator sim = new FixedRateSimulator(bus, 100);
        EventReactor mock = Mockito.mock(EventReactor.class);

        bus.attach(mock);

        final CompletableFuture<Void> runFuture = CompletableFuture.runAsync(() -> {
            Try<Void> running = Try.run(sim::start);
        });

        Thread.sleep(1000);
        sim.shutdown();
        runFuture.get(100, TimeUnit.MILLISECONDS);
        assertTrue(runFuture.isDone());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(mock, atLeast(8)).syncEvent(eq(bus), captor.capture());

        assertThat(captor.getAllValues(), hasItems(instanceOf(Event.ClockTick.class)));
    }
}
