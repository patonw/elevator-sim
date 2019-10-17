package elevator.scheduling;

import elevator.event.EventReactor;
import elevator.model.Elevator;

public interface Scheduler extends EventReactor {
    Scheduler setElevators(Elevator[] elevators);
}
