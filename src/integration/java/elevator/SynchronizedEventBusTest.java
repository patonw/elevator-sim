package elevator;

import elevator.event.*;
import elevator.model.Elevator;
import elevator.model.Floor;
import elevator.model.Passenger;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

// Test that bus delivers messages at all
public class SynchronizedEventBusTest {
    final Floor floor = new Floor(0, 2);
    final Passenger passenger = new Passenger(floor, floor);
    final Elevator elevator = new Elevator(0, 10);
    final Event.ClockTick tickEvent = new Event.ClockTick(2);
    final Event.LoadPassenger loadEvent = new Event.LoadPassenger(floor, elevator, passenger);
    final Event.DropPassenger dropEvent = new Event.DropPassenger(floor, elevator, passenger);

    @Test
    public void testSomething() {
        SynchronizedEventBus bus = new SynchronizedEventBus();
        EventReactor mock = Mockito.mock(EventReactor.class);

        List<Event.ClockTick> ticks = IntStream.range(0, 10).mapToObj(Event.ClockTick::new).collect(Collectors.toList());
        bus.attach(mock);

        bus.fire(ticks.get(0));
        bus.fire(loadEvent);
        bus.processAll();


        bus.fire(ticks.get(1));
        bus.processAll();


        // These are not processed:
        bus.fire(ticks.get(2));
        bus.fire(dropEvent);

        Mockito.verify(mock, times(1)).syncEvent(any(EventBus.class), eq(loadEvent));
        Mockito.verify(mock, times(1)).syncEvent(any(EventBus.class), eq(ticks.get(0)));
        Mockito.verify(mock, times(1)).syncEvent(any(EventBus.class), eq(ticks.get(1)));
        Mockito.verify(mock, times(0)).syncEvent(any(EventBus.class), eq(ticks.get(2)));
        Mockito.verify(mock, times(0)).syncEvent(any(EventBus.class), eq(dropEvent));
    }
}
