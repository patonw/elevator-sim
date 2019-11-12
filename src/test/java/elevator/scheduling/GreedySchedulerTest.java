package elevator.scheduling;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventTopic;
import elevator.model.*;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;

public class GreedySchedulerTest {
    @Test
    public void testAlreadyIncluded() {
        EventBus bus = Mockito.mock(EventBus.class);
        int[] homeFloors = {3, 17, 25};

        GreedyScheduler scheduler = new GreedyScheduler();
        Elevator[] elevators = Stream.range(0, 3).map(i -> Mockito.mock(Elevator.class)).toJavaArray(Elevator.class);

        long currentTime = 20;
        Mockito.when(elevators[0].getTrajectory()).thenReturn(Trajectory.createHoming(homeFloors[0], currentTime, 5).extend(8, 1));    // done at T=20, Floor=1
        Mockito.when(elevators[1].getTrajectory()).thenReturn(Trajectory.createHoming(homeFloors[1], currentTime, 7).extend(2, 15));   // <-- Already traveling this span
        Mockito.when(elevators[2].getTrajectory()).thenReturn(Trajectory.createHoming(homeFloors[2], currentTime, 9).extend(10, 13));  // done at T=24, Floor=13

        scheduler.setElevators(elevators);
        Passenger p1 = new Passenger(14);
        scheduler.onEvent(bus, new Event.ScheduleRequest(p1, 4));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(bus, atLeastOnce()).fireTopic(any(EventTopic.class), captor.capture());

        assertThat(captor.getAllValues(), hasItem(instanceOf(Event.AssignRequest.class)));
        Option<Event.AssignRequest> assignment = List.ofAll(captor.getAllValues())
                .find(ev -> ev instanceof Event.AssignRequest)
                .map(ev -> (Event.AssignRequest) ev);

        assertTrue(assignment.isDefined());
        assertThat(assignment.get().getElevator(), is(1));
        assertThat(assignment.get().getFloor(), is(4));
        assertThat(assignment.get().getPassenger(), is(p1));
    }

    @Test
    public void testOpenScheduling() {
        EventBus bus = Mockito.mock(EventBus.class);
        int[] homeFloors = {3, 17, 25};

        GreedyScheduler scheduler = new GreedyScheduler();
        Elevator[] elevators = Stream.range(0, 3).map(i -> Mockito.mock(Elevator.class)).toJavaArray(Elevator.class);

        long currentTime = 20;
        Mockito.when(elevators[0].getTrajectory()).thenReturn(Trajectory.createHoming(homeFloors[0], currentTime, 5).extend(8, 1));    // done at T=20, Floor=1
        Mockito.when(elevators[1].getTrajectory()).thenReturn(Trajectory.createHoming(homeFloors[1], currentTime, 7).extend(2, 15));   // done at T=38, Floor=15
        Mockito.when(elevators[2].getTrajectory()).thenReturn(Trajectory.createHoming(homeFloors[2], currentTime, 9).extend(10, 13));  // done at T=24, Floor=13

        scheduler.setElevators(elevators);
        Passenger p1 = new Passenger(10);
        scheduler.onEvent(bus, new Event.ScheduleRequest(p1, 20));
        // Elevator 0 arrives at Floor 20 at T=40, returns home at T=57
        // Elevator 1 arrives at Floor 20 at T=43, returns home at T=60
        // Elevator 2 arrives at Floor 20 at T=31, returns home at T=56 <-- winner

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(bus, atLeastOnce()).fireTopic(any(EventTopic.class), captor.capture());

        assertThat(captor.getAllValues(), hasItem(instanceOf(Event.AssignRequest.class)));
        Option<Event.AssignRequest> assignment = List.ofAll(captor.getAllValues())
                .find(ev -> ev instanceof Event.AssignRequest)
                .map(ev -> (Event.AssignRequest) ev);

        assertTrue(assignment.isDefined());
        assertThat(assignment.get().getElevator(), is(2));
        assertThat(assignment.get().getFloor(), is(20));
        assertThat(assignment.get().getPassenger(), is(p1));
    }
}
