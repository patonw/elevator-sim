package elevator.model;

public class IdleBehavior {
    public long timeUntilIdle(Trajectory trajectory) {
        return trajectory.getTimeLeftOnTask();
    }

    public int nextFloor(Trajectory trajectory) {
        return 0;
    }


}
