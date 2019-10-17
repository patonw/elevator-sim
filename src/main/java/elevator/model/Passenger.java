package elevator.model;

import java.util.Objects;
import java.util.UUID;

public class Passenger {
    public final UUID uuid;
    private final int destination;

    // TODO generate a random name for some personality

    public Passenger(int origin, int destination) {
        this.uuid = UUID.randomUUID();
        this.destination = destination;
    }

    public Passenger(int destination) {
        this(0, destination);
    }

    public Passenger(Floor origin, Floor destination) {
        this(origin.getId(), destination.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Passenger passenger = (Passenger) o;
        return uuid.equals(passenger.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    public int getDestination() {
        return destination;
    }

    @Override
    public String toString() {
        return "Passenger " + uuid;
    }
}
