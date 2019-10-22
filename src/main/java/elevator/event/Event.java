package elevator.event;

import elevator.model.Elevator;
import elevator.model.Floor;
import elevator.model.Passenger;
import io.vavr.control.Option;
import net.openhft.chronicle.wire.AbstractMarshallable;

import java.util.Objects;

public interface Event {
    String toMessage();

    class ClockTick extends AbstractMarshallable implements Event {
        private long value;

        public ClockTick(long value) {
            this.value = value;
        }

        public long getValue() {
            return value;
        }

        @Override
        public String toMessage() {
            return String.format("ClockTick(%d)", value);
        }
    }

    class LoadPassenger extends AbstractMarshallable implements Event {
        private int floor;
        private int elevator;
        private Passenger passenger;

        public LoadPassenger(int floor, int elevator, Passenger passenger) {
            this.floor = floor;
            this.elevator = elevator;
            this.passenger = passenger;
        }

        public LoadPassenger(Floor floor, Elevator elevator, Passenger passenger) {
            this(floor.getId(), elevator.getId(), passenger);
        }

        public int getFloor() {
            return floor;
        }

        public int getElevator() {
            return elevator;
        }

        public Passenger getPassenger() {
            return passenger;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LoadPassenger that = (LoadPassenger) o;
            return floor == that.floor &&
                    elevator == that.elevator &&
                    passenger.equals(that.passenger);
        }

        @Override
        public int hashCode() {
            return Objects.hash(floor, elevator, passenger);
        }

        @Override
        public String toMessage() {
            return String.format("LoadPassenger(%s, floor=%d, elevator=%d)", passenger, floor, elevator);
        }
    }

    class DropPassenger extends AbstractMarshallable implements Event {
        private int floor;
        private int elevator;
        private Passenger passenger;

        public DropPassenger(Floor floor, Elevator elevator, Passenger passenger) {
            this(floor.getId(), elevator.getId(), passenger);
        }

        public DropPassenger(int floor, int elevator, Passenger passenger) {
            this.floor = floor;
            this.elevator = elevator;
            this.passenger = passenger;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DropPassenger that = (DropPassenger) o;
            return floor == that.floor &&
                    elevator == that.elevator &&
                    passenger.equals(that.passenger);
        }

        @Override
        public int hashCode() {
            return Objects.hash(floor, elevator, passenger);
        }

        public int getFloor() {
            return floor;
        }

        public int getElevator() {
            return elevator;
        }

        public Passenger getPassenger() {
            return passenger;
        }

        @Override
        public String toMessage() {
            return String.format("DropPassenger(%s, floor=%d, elevator=%d)", passenger, floor, elevator);
        }
    }

    class AssignRequest extends AbstractMarshallable implements Event {
        private final Passenger passenger;
        private final int floor;
        private final int elevator;
        private final Option<Long> timeLeftOnTask;
        private final Option<Long> endTime;

        public AssignRequest(Passenger passenger, int floor, int elevator, Option<Long> timeLeftOnTask, Option<Long> endTime) {
            this.passenger = passenger;
            this.floor = floor;
            this.elevator = elevator;
            this.timeLeftOnTask = timeLeftOnTask;
            this.endTime = endTime;
        }

        public AssignRequest(Passenger passenger, int floor, int elevator) {
            this(passenger, floor, elevator, Option.none(), Option.none());
        }

        public AssignRequest(Passenger passenger, Floor floor, Elevator elevator) {
            this(passenger, floor.getId(), elevator.getId());
        }

        public AssignRequest(Passenger passenger, int floor, int elevator, long timeLeftOnTask) {
            this(passenger, floor, elevator, Option.some(timeLeftOnTask), Option.none());
        }

        public AssignRequest(Passenger passenger, int floor, int elevator, long timeLeftOnTask, long endTime) {
            this(passenger, floor, elevator, Option.some(timeLeftOnTask), Option.some(endTime));
        }


        @Override
        public String toMessage() {
            return String.format("AssignRequest(%s, floor=%d, elevator=%d, deltaT=%s, endTime=%s)", passenger, floor, elevator, timeLeftOnTask, endTime);
        }

        public int getFloor() {
            return floor;
        }

        public Passenger getPassenger() {
            return passenger;
        }

        public int getElevator() {
            return elevator;
        }

        public Option<Long> getTimeLeftOnTask() {
            return timeLeftOnTask;
        }

        public Option<Long> getEndTime() {
            return endTime;
        }
    }

    class ElevatorArrived extends AbstractMarshallable implements Event {
        private final int elevator;
        private final int floor;
        private final long clock;

        public ElevatorArrived(int elevator, int floor, long clock) {
            this.elevator = elevator;
            this.floor = floor;
            this.clock = clock;
        }

        public ElevatorArrived(Elevator elevator, Floor floor) {
            this(elevator.getId(), floor.getId(), 0);
        }

        public int getElevator() {
            return elevator;
        }

        public int getFloor() {
            return floor;
        }

        public long getClock() {
            return clock;
        }

        @Override
        public String toMessage() {
            return String.format("ElevatorArrived(elevator=%d, floor=%d, clock=%d)", elevator, floor, clock);
        }
    }

    class ScheduleRequest extends AbstractMarshallable implements Event {
        private final Passenger passenger;
        private final int start;
        private final int dest;

        public ScheduleRequest(Passenger passenger, int start, int dest) {
            this.passenger = passenger;
            this.start = start;
            this.dest = dest;
        }

        public ScheduleRequest(Passenger passenger, int start) {
            this(passenger, start, passenger.getDestination());
        }

        public Passenger getPassenger() {
            return passenger;
        }

        public int getStart() {
            return start;
        }

        public int getDest() {
            return dest;
        }

        @Override
        public String toMessage() {
            return String.format("ScheduleRequest(%s, start=%d, dest=%d)", passenger, start, dest);
        }
    }

    class RequestAccepted extends AbstractMarshallable implements Event {
        private final AssignRequest request;

        public RequestAccepted(AssignRequest request) {
            this.request = request;
        }

        public AssignRequest getRequest() {
            return request;
        }

        @Override
        public String toMessage() {
            return String.format("RequestAccepted(%s)", request.toMessage());
        }
    }

    class RequestRejected extends AbstractMarshallable implements Event {
        private final AssignRequest request;

        public RequestRejected(AssignRequest request) {
            this.request = request;
        }

        public AssignRequest getRequest() {
            return request;
        }

        @Override
        public String toMessage() {
            return String.format("RequestRejected(%s)", request.toMessage());
        }
    }

    class PassengerWaiting extends AbstractMarshallable implements Event {
        private Passenger passenger;
        private int floor;
        private int elevator;

        public PassengerWaiting(Passenger passenger, int floor, int elevator) {
            this.passenger = passenger;
            this.floor = floor;
            this.elevator = elevator;
        }

        public Passenger getPassenger() {
            return passenger;
        }

        public int getFloor() {
            return floor;
        }

        public int getElevator() {
            return elevator;
        }

        @Override
        public String toMessage() {
            return "PassengerWaiting{" +
                    "passenger=" + passenger +
                    ", floor=" + floor +
                    ", elevator=" + elevator +
                    '}';
        }
    }

    class ElevatorIdle extends AbstractMarshallable implements Event {
        private final int elevator;
        private final int floor;

        public ElevatorIdle(int elevator, int floor) {
            this.elevator = elevator;
            this.floor = floor;
        }

        public int getElevator() {
            return elevator;
        }

        public int getFloor() {
            return floor;
        }

        @Override
        public String toMessage() {
            return String.format("ElevatorIdle(elevator=%d, floor=%d)", elevator, floor);
        }
    }

    class MissedConnection extends AbstractMarshallable implements Event {
        private final int floor;
        private final int elevator;
        private final Passenger passenger;

        public MissedConnection(int floor, int elevatorId, Passenger passenger) {
            this.floor = floor;
            this.elevator = elevatorId;
            this.passenger = passenger;
        }

        public int getFloor() {
            return floor;
        }

        public int getElevator() {
            return elevator;
        }

        public Passenger getPassenger() {
            return passenger;
        }

        @Override
        public String toMessage() {
            return "MissedConnection{" +
                    "floor=" + floor +
                    ", elevator=" + elevator +
                    ", passenger=" + passenger +
                    '}';
        }
    }
}
