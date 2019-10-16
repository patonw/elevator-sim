package elevator.model;

import elevator.event.DeferredEventQueue;
import elevator.model.Building;
import elevator.scheduling.RRFIFOScheduler;
import elevator.scheduling.Scheduler;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

public class BuildingBuilderTest {
    @Test
    public void testSmallBuilding() {
        DeferredEventQueue queue = new DeferredEventQueue();
        Scheduler sched = new RRFIFOScheduler();
        Building result = Building.builder()
                .floors(2)
                .elevators(1)
                .eventQueue(queue)
                .scheduler(sched)
                .build();

        assertThat(result, is(notNullValue()));
        assertThat(result.getNumFloors(), is(2));
        assertThat(result.getNumElevators(), is(1));
        assertThat(result.getScheduler(), is(sched));
        assertThat(result.getEventQueue(), is(queue));
    }

    @Test
    public void testLargelBuilding() {
        DeferredEventQueue queue = new DeferredEventQueue();
        Scheduler sched = new RRFIFOScheduler();
        Building result = Building.builder()
                .floors(100)
                .elevators(10)
                .eventQueue(queue)
                .scheduler(sched)
                .build();

        assertThat(result, is(notNullValue()));
        assertThat(result.getNumFloors(), is(100));
        assertThat(result.getNumElevators(), is(10));
        assertThat(result.getScheduler(), is(sched));
        assertThat(result.getEventQueue(), is(queue));

        assertThat(result.getFloor(99), is(notNullValue()));
        assertThat(result.getElevator(0), is(notNullValue()));
    }
}
