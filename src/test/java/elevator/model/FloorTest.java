package elevator.model;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventTopic;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

public class FloorTest {
    @Test
    public void testRequestAssignment() {
        EventBus bus = Mockito.mock(EventBus.class);
        Floor orig = new Floor(5, 2);
        Floor dest = new Floor(8, 2);
        Passenger passenger = new Passenger(orig, dest);

        Elevator elevator = new Elevator(0, 10);

        orig.onEvent(bus, new Event.RequestAccepted(new Event.AssignRequest(passenger, orig, elevator)));
        dest.onEvent(bus,  new Event.RequestAccepted(new Event.AssignRequest(passenger, orig, elevator)));

        orig.onEvent(bus, new Event.ElevatorArrived(elevator, orig));
        dest.onEvent(bus, new Event.ElevatorArrived(elevator, orig));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(bus, times(2)).fire(any(EventTopic.class), captor.capture());

        assertThat(captor.getValue(), is(instanceOf(Event.LoadPassenger.class)));
    }

}
