package elevator.model;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventTopic;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

public class ElevatorTest {
    @Test
    public void testLoadUnload() {
        EventBus bus = Mockito.mock(EventBus.class);
        Floor orig = new Floor(5,1);
        Floor mid = new Floor(6, 1);
        Floor dest = new Floor(9, 1);
        Passenger passenger = new Passenger(orig, dest);
        Elevator elevator = new Elevator(0, 10);

        assertThat(elevator.getPassengers(), is(empty()));

        elevator.onEvent(bus, new Event.LoadPassenger(orig, elevator, passenger));
        assertThat(elevator.getPassengers(), hasItem(passenger));

        elevator.onEvent(bus, new Event.ElevatorArrived(elevator, mid));
        assertThat(elevator.getPassengers(), hasItem(passenger));

        elevator.onEvent(bus, new Event.ElevatorArrived(elevator, dest));
        assertThat(elevator.getPassengers(), is(empty()));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(bus, times(1)).fire(any(EventTopic.class), captor.capture());

        assertThat(captor.getValue(), is(instanceOf(Event.DropPassenger.class)));
        Event.DropPassenger dropped = (Event.DropPassenger) captor.getValue();
        assertThat(dropped.getPassenger(), is(passenger));
    }

    // TODO test request assignment
    // TODO test clock tick handling
}
