package elevator.model;

public class HomingElevatorFactory extends ElevatorFactory {
    private int[] homeFloors;

    public HomingElevatorFactory(int numFloors, int[] homeFloors) {
        super(numFloors);
        this.homeFloors = homeFloors;
    }

    @Override
    public Elevator create(int id) {
        assert(id < homeFloors.length);
        return new Elevator(id, getNumFloors(),  new HomingTrajectory(homeFloors[id], 0, homeFloors[id]));
    }
}
