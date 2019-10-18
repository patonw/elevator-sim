package elevator;

import elevator.event.*;
import elevator.model.Building;
import elevator.model.HomingElevatorFactory;
import elevator.model.Passenger;
import elevator.scheduling.RRFIFOScheduler;
import elevator.simulation.DeferredEventQueue;
import elevator.simulation.FixedRateSimulator;
import io.vavr.control.Try;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeast;

// Test that homing elevators indeed return home after completing tasks
public class HomingBuildingTest {
    private static final int numFloors = 30;
    private static final int numElevators = 5;
    private static int[] homeFloors = {5, 10, 15, 20, 25};
    private DeferredEventQueue queue;
    private RRFIFOScheduler sched;
    private RunnableEventBus bus;
    private HomingElevatorFactory elevatorFactory;
    private Building building;

    @Before
    public void beforeTest() {
        queue = new DeferredEventQueue();
        sched = new RRFIFOScheduler();
        bus = new PartitionedEventBus();
        elevatorFactory = new HomingElevatorFactory(numFloors, homeFloors);

        building = Building.builder()
                .floors(numFloors)
                .elevators(numElevators)
                .setElevatorFactory(elevatorFactory)
                .setEventBus(bus)
                .eventQueue(queue)
                .scheduler(sched)
                .build();
    }

    @After
    public void afterEach() {
        building = null;
        bus = null;
    }

    @Test
    public void testOneRequest() throws InterruptedException {
        EventReactor mock = Mockito.mock(EventReactor.class);
        bus.attach(mock);

        IntStream.range(0,numElevators).forEach(i -> {
            assertThat(building.getElevator(i).getCurrentFloor(), is(homeFloors[i]));
        });

        FixedRateSimulator sim = new FixedRateSimulator(bus, 20);
        final CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            Try.run(sim::start);
        });

        // Wait for elevators to make it home
        Thread.sleep(500);

        IntStream.range(0,numElevators).forEach(i -> {
            assertThat(building.getElevator(i).getCurrentFloor(), is(homeFloors[i]));
        });

        Passenger p1 = new Passenger(10);
        bus.fire(new Event.ScheduleRequest(p1, 3));

        Thread.sleep(500);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(mock, atLeast(8)).syncEvent(Mockito.any(EventBus.class), captor.capture());
        List<Event> allEvents = captor.getAllValues().stream().filter(ev -> !(ev instanceof Event.ClockTick)).collect(Collectors.toList());
        assertThat(allEvents, hasItems(instanceOf(Event.DropPassenger.class)));

        Optional<Event> loadOpt = allEvents.stream().filter(ev -> (ev instanceof Event.LoadPassenger)).findFirst();
        assertTrue(loadOpt.isPresent());

        Event.LoadPassenger loadEvent = (Event.LoadPassenger) loadOpt.get();
        assertThat(loadEvent.getPassenger(), is(p1));
        assertThat(loadEvent.getFloor(), is(3));

        Optional<Event> dropOpt = allEvents.stream().filter(ev -> (ev instanceof Event.DropPassenger)).findFirst();
        assertTrue(dropOpt.isPresent());

        Event.DropPassenger dropEvent = (Event.DropPassenger) dropOpt.get();
        assertThat(dropEvent.getPassenger(), is(p1));
        assertThat(dropEvent.getFloor(), is(10));

        IntStream.range(0,numElevators).forEach(i -> {
            assertThat(building.getElevator(i).getCurrentFloor(), is(homeFloors[i]));
        });

        sim.shutdown();
        task.join();
    }

    @Test
    public void testConcurrentRequests() throws InterruptedException {
//        LoggingEventListener consoler = new LoggingEventListener();
//        bus.attach(consoler);

        EventReactor mock = Mockito.mock(EventReactor.class);
        bus.attach(mock);

        List<Event> testEvents = new LinkedList<>();

        EventReactor loadDropCapture = (bus, event) -> {
            if (event instanceof Event.LoadPassenger)
                testEvents.add(event);
            else if(event instanceof Event.DropPassenger)
                testEvents.add(event);
        };

        bus.attach(loadDropCapture);

        FixedRateSimulator sim = new FixedRateSimulator(bus, 20);
        final CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            Try.run(sim::start);
        });

        // Wait for elevators to make it home
        Thread.sleep(200);

        Passenger p1 = new Passenger(15);
        bus.fire(new Event.ScheduleRequest(p1, 3));
        Passenger p2 = new Passenger(3);
        bus.fire(new Event.ScheduleRequest(p2, 7));
        Passenger p3 = new Passenger(17);
        bus.fire(new Event.ScheduleRequest(p3, 28));

        Thread.sleep(500);
        assertThat(testEvents, hasItem(is(new Event.LoadPassenger(3, 0, p1))));
        assertThat(testEvents, hasItem(is(new Event.LoadPassenger(7, 1, p2))));
        assertThat(testEvents, hasItem(is(new Event.DropPassenger(3, 1, p2))));
        assertThat(testEvents, hasItem(is(new Event.DropPassenger(15, 0, p1))));

        sim.shutdown();
        task.join();
    }
}
