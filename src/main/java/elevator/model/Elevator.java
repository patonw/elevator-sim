package elevator.model;

import elevator.event.*;
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
    private final int id;

    private final AtomicReference<Trajectory> trajectory = new AtomicReference<>();
    private final ArrayList<Set<Passenger>> floors; // mutable - needs to be synchronized

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

    public synchronized Set<Passenger> getPassengers() {
        return floors.stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    @Override
    public void syncEvent(EventBus bus, Event event) {
        onEvent(bus, event);
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
        else if (event instanceof Event.PassengerWaiting) {
            handlePassengerWaiting(bus, (Event.PassengerWaiting) event);
        }
    }

    private void handlePassengerWaiting(EventBus bus, Event.PassengerWaiting event) {
        final int elevator = event.getElevator();
        if (elevator != this.id)
            return;

        final int floor = event.getFloor();
        if (floor == getCurrentFloor())
            bus.fire(EventTopic.ELEVATOR, new Event.ElevatorArrived(this.id, getCurrentFloor(), getTrajectory().getCurrentTime()));

    }

    private void handleAssignRequest(EventBus bus, Event.AssignRequest event) {
        if (event.getElevator() != this.id)
            return;

        int orig = event.getFloor();
        int dest = event.getPassenger().getDestination();

        while (true) {
            Trajectory oldTraj = getTrajectory();

            // Try inserting into the middle first, only extend if fails
            Trajectory newTraj = oldTraj.augment(orig, dest);

            // Detect and reject stale assignments. May occur if an idle elevator moves to a new floor during scheduling
            // or if another scheduling request is concurrently assigned to this elevator.
            // For a busy elevator that is already on a task, however, remaining time should not vary between clock ticks
            // unless the start of the request is the previous floor.
            if (event.getEndTime().isDefined() && event.getEndTime().get() != newTraj.getEndTime()) {
                bus.fire(EventTopic.SCHEDULING, new Event.RequestRejected(event));
                return;
            }

            // This is not sufficient to achieve optimistic locking. Need to keep coarse-grained sync for now.
            if (trajectory.compareAndSet(oldTraj, newTraj)) {
                log.debug("elevator={} at floor {} accepting request for {} to {}. ", id, getCurrentFloor(), orig, dest);
                log.debug("Trajectory={} changed from {} to {}", getId(), oldTraj, newTraj);
                bus.fire(EventTopic.ELEVATOR, new Event.RequestAccepted(event));
                Thread.yield();

                return;
            }

            // old trajectory has been altered but the result of splicing is no worse
            // Just try the update again
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
                if (getTrajectory().shouldStop())
                    bus.fire(EventTopic.ELEVATOR, new Event.ElevatorArrived(this.id, getCurrentFloor(), now));

                if (oldTraj.isMoving() && getTrajectory().isIdle()) {
                    bus.fire(EventTopic.ELEVATOR, new Event.ElevatorIdle(this.id, getCurrentFloor()));

                    final Set<Passenger> passengers = getPassengers();
                    if (passengers.size() > 0)
                        log.warn("elevator={} idling with stranded passengers {}", id, passengers);
                }

                return;
            }
        }
    }

    private void handleElevatorArrived(EventBus bus, Event.ElevatorArrived event) {
        final int elevatorId = event.getElevator();
        final int floor = event.getFloor();
        if (this.id != elevatorId)
            return;

        unloadPassengers(bus, floor);
    }

    private void unloadPassengers(EventBus bus, int floor) {
        synchronized (floors) {
            assert (floor < floors.size());
            Set<Passenger> toDrop = floors.get(floor);
            if (toDrop.size() <= 0)
                return;

            toDrop.forEach(passenger -> {
                bus.fire(EventTopic.PASSENGER, new Event.DropPassenger(floor, this.getId(), passenger));
            });
            toDrop.clear();
            log.debug("Unloading elevator={} at floor {} with {} remaining", id, floor, getPassengers().size());
        }
    }

    private void handleLoadPassenger(EventBus bus, Event.LoadPassenger event) {
        if (this.id != event.getElevator())
            return;

        synchronized(floors) {
            Passenger passenger = event.getPassenger();
            int dest = passenger.getDestination();

            assert (dest < floors.size());
            floors.get(dest).add(passenger);
            log.debug("Loaded passenger {} total is {}", passenger, getPassengers().size());
            if (!getTrajectory().getTurnpoints().contains(dest))
                log.warn("elevator={} at floor={} is not planning to stop at floor {}... {}!", getId(), getCurrentFloor(), dest, getTrajectory().getTurnpoints());
        }
    }
}
