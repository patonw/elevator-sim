package elevator;

import elevator.event.DeferredEventQueue;
import elevator.event.Event;
import elevator.event.EventReactor;
import elevator.event.SynchronizedEventBus;
import elevator.model.Building;
import elevator.model.HomingElevatorFactory;
import elevator.model.Passenger;
import elevator.scheduling.FlockScheduler;
import elevator.scheduling.Scheduler;
import elevator.simulation.FixedRateSimulator;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

public class FlockSchedulingIntegration {
    private static final int numFloors = 30;
    private static final int numElevators = 5;
    private static int[] homeFloors = {5, 10, 15, 20, 25};
    private DeferredEventQueue queue;
    private Scheduler sched;
    private SynchronizedEventBus bus;
    private HomingElevatorFactory elevatorFactory;
    private Building building;

    @Before
    public void beforeTest() {
        queue = new DeferredEventQueue();
        sched = new FlockScheduler();
        bus = new SynchronizedEventBus();
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

    // All requests made in different clock ticks
    @Test
    public void testSequentialRequests() throws InterruptedException {
//        LoggingEventListener consoler = new LoggingEventListener();
//        bus.attach(consoler);

        final int tickRate = 20;
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

        FixedRateSimulator sim = new FixedRateSimulator(bus, tickRate);
        final CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            Try.run(sim::start);
        });

        // Wait for elevators to make it home
        Thread.sleep(200);

        // Concurrent requests will force scheduling conflicts that will trigger scheduling retries
        Passenger p1 = new Passenger(15);
        bus.fire(new Event.ScheduleRequest(p1, 3));
        Thread.sleep(tickRate);

        Passenger p2 = new Passenger(3);
        bus.fire(new Event.ScheduleRequest(p2, 7));
        Thread.sleep(tickRate);

        Passenger p3 = new Passenger(17);
        bus.fire(new Event.ScheduleRequest(p3, 28));

        Thread.sleep(500);
        assertThat(testEvents, hasItem(is(new Event.LoadPassenger(3, 2, p1))));
        assertThat(testEvents, hasItem(is(new Event.DropPassenger(15, 2, p1))));

        assertThat(testEvents, hasItem(is(new Event.LoadPassenger(7, 0, p2))));
        assertThat(testEvents, hasItem(is(new Event.DropPassenger(3, 0, p2))));

        // TODO verify that there are no rejections

        sim.shutdown();
        task.join();
    }

    // Simultaneous requests, or at least as close as you can get with a blocking queue.
    @Test
    public void testConcurrentRequests() throws InterruptedException {
//        LoggingEventListener consoler = new LoggingEventListener();
//        bus.attach(consoler);

        final int tickRate = 20;
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

        FixedRateSimulator sim = new FixedRateSimulator(bus, tickRate);
        final CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            Try.run(sim::start);
        });

        // Wait for elevators to make it home
        Thread.sleep(200);

        // Concurrent requests will force scheduling conflicts that will trigger scheduling retries
        Passenger p1 = new Passenger(15);
        bus.fire(new Event.ScheduleRequest(p1, 3));
        Passenger p2 = new Passenger(3);
        bus.fire(new Event.ScheduleRequest(p2, 7));
        Passenger p3 = new Passenger(17);
        bus.fire(new Event.ScheduleRequest(p3, 28));

        Thread.sleep(500);
        assertThat(testEvents, hasItem(is(new Event.LoadPassenger(3, 2, p1))));
        assertThat(testEvents, hasItem(is(new Event.DropPassenger(15, 2, p1))));

        assertThat(testEvents, hasItem(is(new Event.LoadPassenger(7, 0, p2))));
        assertThat(testEvents, hasItem(is(new Event.DropPassenger(3, 0, p2))));

        // TODO verify retry events

        sim.shutdown();
        task.join();
    }


    // Within the same clock tick but not simultaneous requests
    @Test
    public void testSemiConcurrentRequests() throws InterruptedException {
        LoggingEventListener consoler = new LoggingEventListener();
        bus.attach(consoler);

        final int tickRate = 20;
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

        FixedRateSimulator sim = new FixedRateSimulator(bus, tickRate);
        final CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            Try.run(sim::start);
        });

        // Wait for elevators to make it home
        Thread.sleep(200);

        // Concurrent requests will force scheduling conflicts that will trigger scheduling retries
        Passenger p1 = new Passenger(15);
        bus.fire(new Event.ScheduleRequest(p1, 3));
        Thread.sleep(tickRate/4);

        Passenger p2 = new Passenger(3);
        bus.fire(new Event.ScheduleRequest(p2, 7));
        Thread.sleep(tickRate/4);

        Passenger p3 = new Passenger(17);
        bus.fire(new Event.ScheduleRequest(p3, 28));

        Thread.sleep(500);
        assertThat(testEvents, hasItem(is(new Event.LoadPassenger(3, 2, p1))));
        assertThat(testEvents, hasItem(is(new Event.DropPassenger(15, 2, p1))));

        assertThat(testEvents, hasItem(is(new Event.LoadPassenger(7, 0, p2))));
        assertThat(testEvents, hasItem(is(new Event.DropPassenger(3, 0, p2))));

        // TODO verify retry events

        sim.shutdown();
        task.join();
    }

}
