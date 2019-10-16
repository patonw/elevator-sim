package elevator.model;

import java.util.concurrent.atomic.AtomicLong;

public class Passenger {
    private static final AtomicLong ctr = new AtomicLong(0);

    // TODO use UUID instead
    public final long id;

    // TODO generate a random name for some personality

    // TODO get rid of mutable state
    private Location location;
    private final Floor destination;

    public Passenger(Floor origin, Floor destination) {
        this.id = ctr.getAndIncrement();
        this.location = origin;
        this.destination = destination;
    }

    public Location getLocation() {
        return location;
    }

    public Floor getDestination() {
        return destination;
    }

    @Override
    public String toString() {
        return "Passenger #" + id;
    }
}
