package elevator.model;

import elevator.util.Splice;
import io.vavr.collection.Queue;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO track home floor and move towards it when idle. Add query for time-to-home
public class Trajectory implements Cloneable {
    private static final Logger log = LoggerFactory.getLogger(Trajectory.class);
    private IdleBehavior idleBehavior;
    private long currentTime = 0;
    private int currentFloor = 0;
    private long timeLeftOnTask = 0;

    private Queue<Integer> turnpoints = Queue.empty();

    protected Trajectory(IdleBehavior idleBehavior, long currentTime, int currentFloor) {
        this.idleBehavior = idleBehavior;
        this.currentTime = currentTime;
        this.currentFloor = currentFloor;
    }

    protected Trajectory(long currentTime, int currentFloor) {
        this(new IdleBehavior(), currentTime, currentFloor);
    }

    public static Trajectory create(long currentTime, int currentFloor) {
        return new Trajectory(new IdleBehavior(), currentTime, currentFloor);
    }

    public static Trajectory createHoming(int homeFloor, long currentTime, int currentFloor) {
        return new Trajectory(new IdleToHome(homeFloor), currentTime, currentFloor);
    }

    @Override
    public String toString() {
        return "Trajectory{" +
                "t=" + currentTime +
                ", floor=" + currentFloor +
                ", turnpoints=" + turnpoints +
                '}';
    }

    protected IdleBehavior getIdleBehavior() {
        return idleBehavior;
    }

    long getCurrentTime() {
        return currentTime;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    // Absolute time when the task is complete
    public long getEndTime() {
        return currentTime + timeLeftOnTask;
    }

    // Relative time until task is complete
    public long getTimeLeftOnTask() {
        return timeLeftOnTask;
    }

    public java.util.List<Integer> getTurnpoints() {
        return turnpoints.toJavaList();
    }

    public boolean isBusy() {
        return timeLeftOnTask > 0;
    }

    public boolean isMoving() {
        return timeUntilIdle() > 0;
    }

    public boolean isIdle() {
        return !isMoving();
    }

    public long timeUntilIdle() {
        return getIdleBehavior().timeUntilIdle(this);
    }

    /**
     * Determine if the elevator should stop on the current floor.
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

    /**
     * Advance the trajectory by one clock cycle and return the result without mutating the current instance.
     *
     * @return A new trajectory instance representing the successor of [this]
     */
    public Trajectory step() {
        try {
            Trajectory result = (Trajectory) this.clone();
            result.turnpoints = result.removeCompleted();
            result.currentFloor = result.nextFloor();
            result.currentTime++;

            if (result.timeLeftOnTask > 0)
                result.timeLeftOnTask--;

            return result;
        } catch (CloneNotSupportedException e) {
            log.warn("Please implement Cloneable on your Trajectory subclass", e);
            return this;
        }
    }

    /**
     * Blindly appends new segment to current turnpoints.
     *
     * @param start
     * @param end
     * @return
     */
    public Trajectory extend(int start, int end) {
        try {
            Trajectory result = (Trajectory) this.clone();
            int oldEndFloor = this.getEndFloor();
            long deltaT = Math.abs(start - oldEndFloor) + Math.abs(end - start);
            result.turnpoints = turnpoints.append(start).append(end);
            result.timeLeftOnTask += deltaT;

//            assert(result.timeLeftOnTask == result.calculateTimeOnTask());

            return result;
        } catch (CloneNotSupportedException e) {
            log.warn("Please implement Cloneable on your Trajectory subclass", e);
            return this;
        }
    }

    /**
     * Inserts new turnpoints if there is any overlap with the current trajectory.
     *
     * @param start
     * @param end
     * @return None if there is no overlap with current trajectory, otherwise a new trajectory.
     */
    Option<Trajectory> insertSegment(int start, int end) {
        if (turnpoints.isEmpty())
            return Option.none();

        Option<Queue<Integer>> spliced = Splice.splice(currentFloor, turnpoints, start, end);
        return spliced.flatMap(newpoints -> Try.of(() -> {
            Trajectory result = (Trajectory) this.clone();
            result.turnpoints = newpoints;

            // Non-strict splice allows trajectory extension if there is an overlap with the end
            if (!newpoints.last().equals(turnpoints.last())) {
                result.timeLeftOnTask += Math.abs(newpoints.last() - turnpoints.last());
//                assert(result.timeLeftOnTask == result.calculateTimeOnTask());
            }

            return result;
        }).toOption());
    }

    private long calculateTimeOnTask() {
        if (turnpoints.isEmpty())
            return 0;

        long result = Math.abs(currentFloor - turnpoints.head());

        result += turnpoints.zip(turnpoints.tail())
                .map(pair -> Math.abs(pair._1 - pair._2))
                .sum()
                .longValue();

        return result;
    }

    /**
     * Adds the directed segment [start,end] to the trajectory.
     *
     * If it's possible to add the segment without appending to the end of the trajectory and increasing the remainingTime,
     * it will do so. However, when not possible, it will just resort to extending the trajectory.
     *
     * @param start Beginning of the interval to add
     * @param end End of the interval to add
     * @return
     */
    public Trajectory augment(int start, int end) {
        return insertSegment(start, end)
                .getOrElse(() -> extend(start,end));
    }

    // Starting a new trajectory on the current floor leads to strange issues
    public Option<Trajectory> augmentOpt(int start, int end) {
        if (start == currentFloor)
            return Option.none();
        else
            return Option.some(augment(start,end));
    }


    public boolean includes(int start, int end) {
        return insertSegment(start, end)
                .map(newT -> newT.getTimeLeftOnTask() == this.getTimeLeftOnTask())
                .getOrElse(false);
    }

    public int nextFloor() {
        final Queue<Integer> nextPoints = removeCompleted(); // maybe memoize?

        if (nextPoints.nonEmpty())
            return currentFloor + Integer.compare(nextPoints.head(), currentFloor);
        else
            return currentFloor + getIdleBehavior().nextFloor(this);
    }

    public int getEndFloor() {
        if (turnpoints.isEmpty())
            return currentFloor;

        return turnpoints.last();
    }

}
