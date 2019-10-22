package elevator.model;

import elevator.event.EventBus;
import elevator.event.EventReactor;
import elevator.event.EventTopic;
import elevator.scheduling.RejectionReactor;
import elevator.scheduling.ReschedulingReactor;
import elevator.scheduling.Scheduler;
import elevator.simulation.DeferredEventQueue;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

import static java.util.stream.IntStream.range;

public class Building implements Cloneable {
    private static final Logger log = LoggerFactory.getLogger(Building.class);
    private Floor[] floors;
    private Elevator[] elevators;
    private DeferredEventQueue eventQueue;
    private EventBus bus;
    private Scheduler scheduler;

    protected Building() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public int getNumFloors() {
        return floors.length;
    }

    public int getNumElevators() {
        return elevators.length;
    }

    public Floor getFloor(int i) {
        assert(i >= 0 && i < floors.length);
        return floors[i];
    }

    public Elevator getElevator(int i) {
        assert(i >= 0 && i < elevators.length);
        return elevators[i];
    }

    public DeferredEventQueue getEventQueue() {
        return eventQueue;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public static class Builder {
        private Building result = new Building();
        private int numFloors;
        private int numElevators;
        private ElevatorFactory elevatorFactory;
        private EventReactor rejectionHandler = new RejectionReactor();
        private EventReactor reschedReactor = new ReschedulingReactor();
        private List<EventReactor> reactors = List.empty();

        public Builder setElevatorFactory(ElevatorFactory factory) {
            this.elevatorFactory = factory;
            return this;
        }

        public Builder setEventBus(EventBus bus) {
            result.bus = bus;
            return this;
        }

        public Builder floors(int n) {
            numFloors = n;
            return this;
        }

        public Builder elevators(int n) {
            numElevators = n;

            return this;
        }

        public Builder eventQueue(DeferredEventQueue queue) {
            result.eventQueue = queue;
            return this;
        }

        public Builder scheduler(Scheduler sched) {
            result.scheduler = sched;
            return this;
        }

        public Building build() {
            if (this.numFloors <= 0)
                throw new IllegalStateException("Floors not set");

            if (this.numElevators <= 0)
                throw new IllegalStateException("Number of elevators not set");

            if (result.bus == null)
                throw new IllegalStateException("Event bus not set");

            if (result.scheduler == null)
                throw new IllegalStateException("Scheduler not set");

            if (result.eventQueue == null)
                throw new IllegalStateException("Event Queue not set");

            if (this.elevatorFactory == null)
                elevatorFactory = new ElevatorFactory(numFloors);

            try {
                Building clone = (Building) result.clone();
                EventBus bus = clone.bus;
                bus.attach(clone.eventQueue);
                bus.attachTopic(EnumSet.of(EventTopic.DEFAULT, EventTopic.SCHEDULING), clone.scheduler);

                // TODO expose builders
                bus.attachTopic(EnumSet.of(EventTopic.SCHEDULING), rejectionHandler);
                bus.attachTopic(EnumSet.of(EventTopic.PASSENGER), reschedReactor);
                reactors.forEach(bus::attach);

                clone.floors = new Floor[numFloors];
                range(0,numFloors).forEach(i -> {
                    clone.floors[i] = new Floor(i, numElevators);
                    bus.attachTopic(EnumSet.of(EventTopic.DEFAULT, EventTopic.ELEVATOR), clone.floors[i]);
                });

                clone.elevators = new Elevator[numElevators];
                range(0, numElevators).forEach(i -> {
                    clone.elevators[i] = elevatorFactory.create(i);
                    bus.attach(clone.elevators[i]);
                });

                clone.scheduler.setElevators(clone.elevators);


                return clone;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Building is not cloneable", e);
            }
        }

    }
}
