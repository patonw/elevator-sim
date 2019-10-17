package elevator.model;

public class ElevatorFactory {
    private int numFloors;

    public ElevatorFactory(int numFloors) {
        this.numFloors = numFloors;
    }

    public int getNumFloors() {
        return numFloors;
    }

    public Elevator create(int id) {
        return new Elevator(id, numFloors, new Trajectory(0,0));
    }
}
