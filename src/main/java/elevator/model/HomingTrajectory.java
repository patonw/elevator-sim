package elevator.model;

public class HomingTrajectory extends Trajectory implements Cloneable {
    private int homeFloor;
    public HomingTrajectory(int homeFloor, long currentTime, int currentFloor) {
        super(currentTime, currentFloor);
        this.homeFloor = homeFloor;
    }

    public long timeToHome() {
        if (getTimeLeftOnTask() > 0)
            return getTimeLeftOnTask() + Math.abs(getEndFloor() - homeFloor);
        else
            return Math.abs(getCurrentFloor() - homeFloor);
    }

    @Override
    public boolean isMoving() {
        return timeToHome() > 0;
    }

    @Override
    public boolean isIdle() {
        return timeToHome() <= 0;
    }

    @Override
    public long timeUntilIdle() {
        return timeToHome();
    }

    @Override
    public HomingTrajectory step() {
        return (HomingTrajectory) super.step();
    }

    @Override
    public HomingTrajectory extend(int start, int end) {
        return (HomingTrajectory) super.extend(start, end);
    }

    @Override
    public HomingTrajectory augment(int start, int end) {
        return (HomingTrajectory) super.augment(start, end);
    }

    @Override
    public int nextFloor() {
        if (getTimeLeftOnTask() > 0)
            return super.nextFloor();
        else if (homeFloor == getCurrentFloor())
            return homeFloor;
        else if (homeFloor > getCurrentFloor())
            return getCurrentFloor()+1;
        else
            return getCurrentFloor()-1;
    }

}
