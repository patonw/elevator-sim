package elevator.model;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventReactor;
import elevator.event.EventTopic;
import io.vavr.Tuple2;
import io.vavr.collection.*;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Elevator implements Location, EventReactor {
    private static AtomicLong ctr = new AtomicLong(0);

    public final long id;
    private AtomicLong clock = new AtomicLong(); // TODO use trajectory's clock instead. Just make sure it matches clock events

    private final AtomicReference<Set<Floor>> stops = new AtomicReference<>(HashSet.empty()); // TODO query trajectory instead
    private final AtomicReference<Set<Passenger>> passengers = new AtomicReference<>(HashSet.empty()); // TODO use an array of [floor] -> Set<Passenger>
    private final AtomicReference<Trajectory> trajectory = new AtomicReference<>(new Trajectory(0,0));

    // TODO take id and number of floors as params
    public Elevator() {
        id = ctr.getAndIncrement();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Elevator elevator = (Elevator) o;
        return id == elevator.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public java.util.List<Floor> getStops() {
        return stops.get().toJavaList();
    }

    @Override
    public String toString() {
        return "Elevator #" + id;
    }

    public Trajectory getTrajectory() {
        return trajectory.get();
    }

    public java.util.List<Passenger> getPassengers() {
        return passengers.get().toJavaList();
    }

    @Override
    public void onEvent(EventBus bus, Event event) {
        if (event instanceof Event.ClockTick) {
            handleClockTick(bus, (Event.ClockTick)event);
        }
        else if (event instanceof Event.LoadPassenger) {
            handleLoadPassenger(bus, (Event.LoadPassenger) event);
        }
        else if (event instanceof Event.ElevatorArrived) {
            handleElevatorArrived(bus, (Event.ElevatorArrived) event);
        }
    }

    private void handleClockTick(EventBus bus, Event.ClockTick event) {
        final long now = event.getValue();

        while (true) {
            long oldClock = clock.get();
            if (oldClock >= now)
                break;

            long newClock = oldClock + 1;
            if (clock.compareAndSet(oldClock, newClock)) {
                processCurrentTime(bus);
            }
        }
    }

    private void processCurrentTime(EventBus bus) {
        // Update position based on trajectory
        // Fire arrival event if floor is on stops
    }

    private void handleElevatorArrived(EventBus bus, Event.ElevatorArrived event) {
        final Elevator elevator = event.getElevator();
        final Floor floor = event.getFloor();
        if (!this.equals(elevator))
            return;

        while (true) {
            final Set<Passenger> oldValue = passengers.get();
            final Tuple2<? extends Set<Passenger>, ? extends Set<Passenger>> partition = oldValue.partition(passenger -> passenger.getDestination().equals(floor));
            Set<Passenger> departing = partition._1, remaining = partition._2;

            if (passengers.compareAndSet(oldValue, remaining)) {
                departing.forEach(passenger -> {
                    bus.fire(EventTopic.PASSENGER, new Event.DropPassenger(floor, elevator, passenger));
                });

                break;
            }
        }
    }

    private void handleLoadPassenger(EventBus bus, Event.LoadPassenger event) {
        if (!this.equals(event.getElevator()))
            return;

        while (true) {
            final Set<Passenger> oldValue = passengers.get();
            final Set<Passenger> newValue = oldValue.add(event.getPassenger());
            if (passengers.compareAndSet(oldValue, newValue))
                break;
        }
    }
}
