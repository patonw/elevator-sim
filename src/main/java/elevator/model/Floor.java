package elevator.model;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventReactor;
import elevator.event.EventTopic;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Floor implements Location, EventReactor {
    // TODO rename to id
    private final int number;
    private Map<Elevator, List<Passenger>> waiting = new HashMap<>(); // TODO use an array of [Elevator] -> Set<Passenger>
//    private AtomicReference<Map<Elevator, List<Passenger>>> ref = new AtomicReference<>(HashMap.empty()); // vavr-ized

    // TODO parameterize with number of elevators
    public Floor(int number) {
        this.number = number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Floor floor = (Floor) o;
        return number == floor.number;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }

    @Override
    public String toString() {
        return "Floor #" + number;
    }

    public int getNumber() {
        return number;
    }

    @Override
    public void onEvent(EventBus bus, Event event) {
        if (event instanceof Event.RequestAssignment) {
            handleRequestAssignment(bus, (Event.RequestAssignment) event);
        }
        else if (event instanceof Event.ElevatorArrived) {
            handleElevatorArrived(bus, (Event.ElevatorArrived) event);
        }
    }

    private void handleElevatorArrived(EventBus bus, Event.ElevatorArrived event) {
        if (!this.equals(event.getFloor()))
            return;

        Elevator elevator = event.getElevator();
        List<Passenger> waiters = waiting.computeIfAbsent(elevator, key -> new LinkedList<>());
        waiters.forEach(passenger -> {
            bus.fire(EventTopic.PASSENGER, new Event.LoadPassenger(this, elevator, passenger));
        });

        waiters.clear();
    }

    private void handleRequestAssignment(EventBus bus, Event.RequestAssignment event) {
        if (!this.equals(event.getFloor()))
            return;

        Elevator elevator = event.getElevator();
        Passenger passenger = event.getPassenger();
        List<Passenger> waiters = waiting.computeIfAbsent(elevator, key -> new LinkedList<>());
        waiters.add(passenger);
    }
}
