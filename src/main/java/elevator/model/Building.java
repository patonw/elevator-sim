package elevator.model;

import elevator.scheduling.Scheduler;
import elevator.event.DeferredEventQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.IntStream.range;

public class Building implements Cloneable {
    private static final Logger log = LoggerFactory.getLogger(Building.class);
    private Floor[] floors;
    private Elevator[] elevators;
    private DeferredEventQueue eventQueue;
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

        public Builder floors(int n) {
            result.floors = new Floor[n];
            range(0,n).forEach(i -> {
                result.floors[i] = new Floor(i);
            });

            return this;
        }

        public Builder elevators(int n) {
            result.elevators = new Elevator[n];
            range(0, n).forEach(i -> {
                result.elevators[i] = new Elevator();
            });

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
            if (result.floors == null)
                throw new IllegalStateException("Floors not set");

            if (result.elevators == null)
                throw new IllegalStateException("Elevators not set");

            if (result.scheduler == null)
                throw new IllegalStateException("Scheduler not set");

            if (result.eventQueue == null)
                throw new IllegalStateException("Event Queue not set");

            try {
                return (Building) result.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Building is not cloneable", e);
            }
        }
    }
}
