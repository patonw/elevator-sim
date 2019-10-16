package elevator.model;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventReactor;
import elevator.event.EventTopic;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

public class FloorTest {
    @Test
    public void testRequestAssignment() {
        EventBus bus = Mockito.mock(EventBus.class);
        Floor orig = new Floor(5);
        Floor dest = new Floor(8);
        Passenger passenger = new Passenger(orig, dest);

        Elevator elevator = new Elevator();

        orig.onEvent(bus, new Event.RequestAssignment(orig, passenger, elevator));
        dest.onEvent(bus, new Event.RequestAssignment(orig, passenger, elevator));

        orig.onEvent(bus, new Event.ElevatorArrived(elevator, orig));
        dest.onEvent(bus, new Event.ElevatorArrived(elevator, orig));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(bus, times(1)).fire(any(EventTopic.class), captor.capture());

        assertThat(captor.getValue(), is(instanceOf(Event.LoadPassenger.class)));
    }

}
