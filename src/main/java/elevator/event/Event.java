package elevator.event;

import elevator.model.Elevator;
import elevator.model.Floor;
import elevator.model.Passenger;

public interface Event {

    class ClockTick implements Event {
        private long value;

        public ClockTick(long value) {
            this.value = value;
        }

        public long getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    class LoadPassenger implements Event {
        private Floor floor;
        private Elevator elevator;
        private Passenger passenger;

        public LoadPassenger(Floor floor, Elevator elevator, Passenger passenger) {
            this.floor = floor;
            this.elevator = elevator;
            this.passenger = passenger;
        }

        public Floor getFloor() {
            return floor;
        }

        public Elevator getElevator() {
            return elevator;
        }

        public Passenger getPassenger() {
            return passenger;
        }
    }

    class DropPassenger implements Event {
        private Floor floor;
        private Elevator elevator;
        private Passenger passenger;

        public DropPassenger(Floor floor, Elevator elevator, Passenger passenger) {
            this.floor = floor;
            this.elevator = elevator;
            this.passenger = passenger;
        }

        public Floor getFloor() {
            return floor;
        }

        public Elevator getElevator() {
            return elevator;
        }

        public Passenger getPassenger() {
            return passenger;
        }
    }

    class RequestAssignment implements Event {
        private final Floor floor;
        private final Passenger passenger;
        private final Elevator elevator;

        public RequestAssignment(Floor floor, Passenger passenger, Elevator elevator) {
            this.floor = floor;
            this.passenger = passenger;
            this.elevator = elevator;
        }

        public Floor getFloor() {
            return floor;
        }

        public Passenger getPassenger() {
            return passenger;
        }

        public Elevator getElevator() {
            return elevator;
        }
    }

    class ElevatorArrived implements Event {
        private final Elevator elevator;
        private final Floor floor;

        public ElevatorArrived(Elevator elevator, Floor floor) {
            this.elevator = elevator;
            this.floor = floor;
        }

        public Elevator getElevator() {
            return elevator;
        }

        public Floor getFloor() {
            return floor;
        }
    }
}
