package elevator.model;

import io.vavr.collection.Queue;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO double check immutability
// TODO track home floor and move towards it when idle. Add query for time-to-home
public class Trajectory implements Cloneable {
    private static final Logger log = LoggerFactory.getLogger(Trajectory.class);
    private long currentTime = 0; // TODO Only use this for sanity check
    private int currentFloor = 0;
    private long remainingTime = 0;

    private Queue<Integer> turnpoints = Queue.empty();

    public Trajectory(long currentTime, int currentFloor) {
        this.currentTime = currentTime;
        this.currentFloor = currentFloor;
    }

    long getCurrentTime() {
        return currentTime;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    long getEndTime() {
        return currentTime + remainingTime;
    }

    public long getRemainingTime() {
        return remainingTime;
    }

    public java.util.List<Integer> getTurnpoints() {
        return turnpoints.toJavaList();
    }

    /**
     *
     * @return True when the elevator should stop at the current floor
     */
    public boolean shouldStop() {
        return turnpoints.nonEmpty() && turnpoints.head() == currentFloor;
    }

    private Queue<Integer> removeCompleted() {
        Queue<Integer> nextpoints = turnpoints;
        while (nextpoints.nonEmpty() && nextpoints.head() == currentFloor)
            nextpoints = nextpoints.tail();

        return nextpoints;
    }

    public Trajectory step() {
        try {
            Trajectory result = (Trajectory) this.clone();
            result.turnpoints = result.removeCompleted();
            result.currentFloor = result.nextFloor();
            result.currentTime++;

            if (result.remainingTime > 0)
                result.remainingTime--;

            return result;
        } catch (CloneNotSupportedException e) {
            log.warn("Please implement Cloneable on your Trajectory subclass", e);
            return this;
        }
    }

    public Trajectory extend(int start, int end) {
        try {
            Trajectory result = (Trajectory) this.clone();
            int oldEndFloor = this.getEndFloor();
            long deltaT = Math.abs(start - oldEndFloor) + Math.abs(end - start);
            result.turnpoints = turnpoints.append(start).append(end);
            result.remainingTime += deltaT;
//            result.removeCompleted();

            return result;
        } catch (CloneNotSupportedException e) {
            log.warn("Please implement Cloneable on your Trajectory subclass", e);
            return this;
        }
    }

    // Adds turnpoints to trajectory if possible without increasing remaining time
    public Option<Trajectory> insertSegment(int start, int end) {
        return Option.none();
    }

    public boolean includes(int start, int end) {
        if (turnpoints.isEmpty())
            return false;

        if (start < end) {
            // Heading up
            return turnpoints
                    .dropWhile(i -> i > start)
                    .find(i -> i >= end)
                    .isDefined();
        }
        else {
            // going down
            return turnpoints
                    .dropWhile(i -> i < start)
                    .find(i -> i <= end)
                    .isDefined();
        }
    }

    public int nextFloor() {
        final Queue<Integer> nextPoints = removeCompleted(); // maybe memoize?
        if (nextPoints.isEmpty())
            return currentFloor; // idle
        else if (nextPoints.head() > currentFloor)
            return currentFloor + 1;
        else
            return currentFloor - 1;
    }

    public int getEndFloor() {
        if (turnpoints.isEmpty())
            return currentFloor;

        return turnpoints.last();
    }
}
