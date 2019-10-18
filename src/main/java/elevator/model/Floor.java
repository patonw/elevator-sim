package elevator.model;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventReactor;
import elevator.event.EventTopic;

import java.util.*;
import java.util.stream.Collectors;

public class Floor implements EventReactor {
    private final int id;
    private final ArrayList<Set<Passenger>> elevators;

    public Floor(int id, int numElevators) {
        this.id = id;
        this.elevators = new ArrayList<>(numElevators);
        for (int i = 0; i < numElevators; i++) {
            elevators.add(i, new HashSet<>());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Floor floor = (Floor) o;
        return id == floor.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Floor #" + id;
    }

    public int getId() {
        return id;
    }

    public Set<Passenger> getPassengers() {
        return elevators.stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    @Override
    public void onEvent(EventBus bus, Event event) {
        if (event instanceof Event.ElevatorArrived) {
            handleElevatorArrived(bus, (Event.ElevatorArrived) event);
        }
        else if (event instanceof Event.RequestAccepted) {
            handleRequestAccepted(bus, (Event.RequestAccepted) event);
        }

        // TODO log arriving passengers for audit
    }

    private void handleRequestAccepted(EventBus bus, Event.RequestAccepted event) {
        handleRequestAssignment(bus, event.getRequest());
    }

    private void handleElevatorArrived(EventBus bus, Event.ElevatorArrived event) {
        if (this.getId() != event.getFloor())
            return;

        int elevatorId = event.getElevator();
        synchronized (elevators) {
            Set<Passenger> toLoad = elevators.get(elevatorId);
            toLoad.forEach(passenger -> {
                bus.fire(EventTopic.PASSENGER, new Event.LoadPassenger(this.getId(), elevatorId, passenger));
            });
            toLoad.clear();
        }
    }

    private void handleRequestAssignment(EventBus bus, Event.AssignRequest event) {
        if (this.getId() != event.getFloor())
            return;

        synchronized (elevators) {
            int elevatorId = event.getElevator();
            Passenger passenger = event.getPassenger();
            assert (elevatorId < elevators.size());
            elevators.get(elevatorId).add(passenger);
        }
    }
}
