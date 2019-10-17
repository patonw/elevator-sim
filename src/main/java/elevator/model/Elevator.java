package elevator.model;

import elevator.event.*;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Elevator implements EventReactor {
    private static final Logger log = LoggerFactory.getLogger(Elevator.class);
    public final int id;

    private final AtomicReference<Trajectory> trajectory = new AtomicReference<>();
    private ArrayList<Set<Passenger>> floors;

    public Elevator(int id, int numFloors, Trajectory trajectory) {
        this.id = id;
        this.floors = new ArrayList<>(numFloors);

        IntStream.range(0, numFloors).forEach(i-> {
            floors.add(i, new HashSet<>());
        });

        this.trajectory.set(trajectory);
    }

    public Elevator(int id, int numFloors) {
        this(id, numFloors, new Trajectory(0,0));
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

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Elevator #" + id;
    }

    public int getCurrentFloor() {
        return getTrajectory().getCurrentFloor();
    }

    public Trajectory getTrajectory() {
        return trajectory.get();
    }

    public Set<Passenger> getPassengers() {
        return floors.stream().flatMap(Collection::stream).collect(Collectors.toSet());
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
        else if (event instanceof Event.AssignRequest) {
            handleAssignRequest(bus, (Event.AssignRequest) event);
        }
    }

    private void handleAssignRequest(EventBus bus, Event.AssignRequest event) {
        if (event.getElevator() != this.id)
            return;

        int orig = event.getFloor();
        int dest = event.getPassenger().getDestination();

        while (true) {
            Trajectory oldTraj = getTrajectory();

            // Try inserting into the middle first, only extend if fails
            Option<Trajectory> inserted = oldTraj.insertSegment(orig, dest);
            Trajectory newTraj = inserted.getOrElse(() -> oldTraj.extend(orig, dest));

            // Detect and reject stale assignments. May occur if an idle elevator moves to a new floor during scheduling
            // or if another scheduling request is concurrently assigned to this elevator.
            // For a busy elevator that is already on a task, however, remaining time should not vary between clock ticks
            // unless the start of the request is the previous floor.
            if (event.getTimeLeftOnTask().isDefined() && event.getTimeLeftOnTask().get() != newTraj.getTimeLeftOnTask()) {
                bus.fire(EventTopic.SCHEDULING, new Event.RequestRejected(event));
                return;
            }

            // This is not sufficient to achieve optimistic locking. Need to keep coarse-grained sync for now.
            if (trajectory.compareAndSet(oldTraj, newTraj)) {
                bus.fire(EventTopic.SCHEDULING, new Event.RequestAccepted(event));
                processCurrentTime(bus);
                return;
            }
        }
    }

    private void handleClockTick(EventBus bus, Event.ClockTick event) {
        final long now = event.getValue();

        while (true) {
            Trajectory oldTraj = getTrajectory();
            if (oldTraj.getCurrentTime()>= now) {
                log.warn("Spurious clock tick {}", event);
                return;
            }

            Trajectory newTraj = getTrajectory().step();

            assert(newTraj.getCurrentTime() == now);
            if (trajectory.compareAndSet(oldTraj, newTraj)) {
                processCurrentTime(bus);

                if (oldTraj.isMoving() && getTrajectory().isIdle())
                    bus.fire(EventTopic.ELEVATOR, new Event.ElevatorIdle(this.id, getCurrentFloor()));

                return;
            }
        }
    }

    private void processCurrentTime(EventBus bus) {
        // Update position based on trajectory
        // Fire arrival event if floor is on stops
        if (getTrajectory().shouldStop())
            bus.fire(EventTopic.ELEVATOR, new Event.ElevatorArrived(this.id, getCurrentFloor()));
    }

    private void handleElevatorArrived(EventBus bus, Event.ElevatorArrived event) {
        final int elevatorId = event.getElevator();
        final int floor = event.getFloor();
        if (this.id != elevatorId)
            return;

        assert (floor < floors.size());
        Set<Passenger> toDrop = floors.get(floor);
        toDrop.forEach(passenger -> {
            bus.fire(EventTopic.PASSENGER, new Event.DropPassenger(floor, this.getId(), passenger));
        });
        toDrop.clear();
    }

    private void handleLoadPassenger(EventBus bus, Event.LoadPassenger event) {
        if (this.id != event.getElevator())
            return;

        Passenger passenger = event.getPassenger();
        int dest = passenger.getDestination();

        assert (dest < floors.size());
        floors.get(dest).add(passenger);
    }
}
