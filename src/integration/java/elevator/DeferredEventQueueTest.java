package elevator;

import elevator.*;
import elevator.event.DeferredEventQueue;
import elevator.event.Event;
import elevator.event.EventReactor;
import elevator.event.SynchronizedEventBus;
import elevator.model.Elevator;
import elevator.model.Floor;
import elevator.model.Passenger;
import elevator.simulation.OfflineSimulator;
import org.junit.Test;
import org.mockito.Mockito;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.times;

public class DeferredEventQueueTest {
    final Floor floor = new Floor(0, 1);
    final Passenger passenger = new Passenger(floor, floor);

    final Elevator elevator = new Elevator(0, 10);
    final Event.LoadPassenger loadEvent = new Event.LoadPassenger(floor, elevator, passenger);
    final Event.DropPassenger dropEvent = new Event.DropPassenger(floor, elevator, passenger);

    @Test
    public void testSimulation() {
        SynchronizedEventBus bus = new SynchronizedEventBus();
        OfflineSimulator sim = new OfflineSimulator(bus);
        DeferredEventQueue queue = new DeferredEventQueue();
        EventReactor mock = Mockito.mock(EventReactor.class);

        bus.attach(mock);
        bus.attach(queue);

        queue.scheduleAt(5, loadEvent);
        queue.scheduleAt(10, dropEvent);

        sim.runTo(10);

        Mockito.verify(mock, times(1)).syncEvent(bus, loadEvent);
        Mockito.verify(mock, times(1)).syncEvent(bus, dropEvent);
        assertThat(queue.getClock(), is(10L));
    }


    @Test(expected = IllegalArgumentException.class)
    public void testTimeTravelIsNotAllowed() {
        SynchronizedEventBus bus = new SynchronizedEventBus();
        OfflineSimulator sim = new OfflineSimulator(bus);
        DeferredEventQueue queue = new DeferredEventQueue();
        EventReactor mock = Mockito.mock(EventReactor.class);

        bus.attach(mock);
        bus.attach(queue);

        queue.scheduleAt(5, loadEvent);
        sim.runTo(5);
        Mockito.verify(mock, times(1)).syncEvent(bus, loadEvent);

        queue.scheduleAt(1, dropEvent); // throws here
    }

    @Test
    public void testOutOfOrderSchedulingIsFine() throws InterruptedException {
        SynchronizedEventBus bus = new SynchronizedEventBus();
        DeferredEventQueue queue = new DeferredEventQueue();
        EventReactor mock = Mockito.mock(EventReactor.class);
        OfflineSimulator sim = new OfflineSimulator(bus);

        bus.attach(mock);
        bus.attach(queue);
        bus.attach(new LoggingEventListener());

        queue.scheduleAt(5, dropEvent);
        queue.scheduleAt(1, loadEvent);
        sim.runTo(10);

        Mockito.verify(mock, times(1)).syncEvent(bus, loadEvent);
        Mockito.verify(mock, times(1)).syncEvent(bus, dropEvent);
        assertThat(queue.getClock(), is(10L));
    }

}
