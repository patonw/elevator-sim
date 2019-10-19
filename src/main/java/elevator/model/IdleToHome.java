package elevator.model;

public class IdleToHome extends IdleBehavior {
    private final int homeFloor;

    public IdleToHome(int homeFloor) {
        this.homeFloor = homeFloor;
    }

    public int getHomeFloor() {
        return homeFloor;
    }

    @Override
    public long timeUntilIdle(Trajectory trajectory) {
        if (trajectory.getTimeLeftOnTask() > 0)
            return trajectory.getTimeLeftOnTask() + Math.abs(trajectory.getEndFloor() - getHomeFloor());
        else
            return Math.abs(trajectory.getCurrentFloor() - getHomeFloor());
    }

    @Override
    public int nextFloor(Trajectory trajectory) {
        return Integer.compare(homeFloor, trajectory.getCurrentFloor());
    }
}
