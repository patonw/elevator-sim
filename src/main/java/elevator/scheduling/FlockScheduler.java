package elevator.scheduling;

import elevator.event.Event;
import elevator.event.EventBus;
import elevator.event.EventTopic;
import elevator.model.Elevator;
import elevator.model.Trajectory;

import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

/**
 * Greedily minimizes time until all elevators return home/become idle.
 *
 * This will work with either homing elevators or standard elevators that idle at their last
 * destination.
 *
 * The augmented trajectory with the lowest timeUntilIdle will add the least to the global
 * timeUntilIdle if at all.
 */
public class FlockScheduler implements Scheduler {
    private Elevator[] elevators;

    @Override
    public Scheduler setElevators(Elevator[] elevators) {
        this.elevators = elevators;
        return this;
    }

    @Override
    public void onEvent(EventBus bus, Event event) {
        if (event instanceof Event.ScheduleRequest) {
            handleScheduleRequest(bus, (Event.ScheduleRequest) event);
        }
    }

    private void handleScheduleRequest(EventBus bus, Event.ScheduleRequest event) {
        final int start = event.getStart();
        final int dest = event.getDest();

        List<Trajectory> oldTrajectories = Stream.range(0, elevators.length)
                .map(i -> elevators[i])
                .map(Elevator::getTrajectory)
                .toList();

        List<Trajectory> newTrajectories = oldTrajectories.map(t -> t.augment(start, dest));

        Option<Tuple2<Trajectory, Integer>> bestTrajectory = newTrajectories
                .zipWithIndex()     // Add the index
                .minBy(pair -> pair._1.timeUntilIdle());  // Minimize by timeToHome

        Integer assignee = bestTrajectory.get()._2;
        long timeLeftOnTask = bestTrajectory.get()._1.getTimeLeftOnTask();
        long endTime = bestTrajectory.get()._1.getEndTime();
        bus.fire(EventTopic.SCHEDULING, new Event.AssignRequest(event.getPassenger(), event.getStart(), assignee, timeLeftOnTask, endTime));
    }
}
