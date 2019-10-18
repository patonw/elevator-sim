# Elevator Simulator

## Usage
Run via Gradle wrapper:

> $ ./gradlew run

This starts the simulator, logging events to the console. The simulator accepts requests over HTTP on port 7000.

You can make individual requests on the `/passenger` endpoint using any HTTP client, such as curl:
> $ curl "http://localhost:7000/passenger?origin=30&dest=84"

Parameters:

- `origin` is the floor the passenger starts on
- `dest` is the floor number the passenger is trying to get to

Passengers are distinguished by randomly generated UUIDs and there is no name parameter.
Application events are just dumped to the console however since I haven't gotten around to writing a rich client.

Additionally, there is a stress testing endpoint at `/stress` which takes `n` the number of total requests and `rate`
which is the number of requests per clock tick. When the `concurrent` flag is set, all requests within a given clock
tick are fired at the same time. Otherwise requests are spread out within the clock tick. You can use this to trigger
assignment rejection and scheduling retries as explained in [Event System](#Event_System). The origin and destination
floors are randomly generated for each passenger.

> $ curl "http://localhost:7000/stress?rate=10&n=100"

At the beginning of the simulation elevators start on their home floor as defined by `App::HOME_FLOORS`.
Passengers' requests are assigned to an elevator by the scheduler (more below).
When an elevator is finished serving requests it will move back towards its home floor.
The return to home, behavior can be disabled by [Configuration](#Configuration).

## Design
### Event System
The elevator simulator uses an event driven architecture to decouple the components of the domain model and scheduler.
Floors, elevators and schedulers attach to the event bus and react to events by firing subsequent messages.
These domain objects are instances of the `EventReactor` interface which is intended to denote objects that can only react
to events but cannot spontaneously fire unsolicited events. Anything capable of emitting spontaneous events should be marked
with the EventEmitter interface, although all that is really required is having a persistent reference to the bus.

The default event bus implementation is `SynchronizedEventBus` which dispatches events to the `syncEvent` method of each reactor.
The default behavior is to use a coarse-grained lock on the entire reactor object to prevent concurrent modification on
each individual reactor. Since state mutation occurs through the event system, this is sufficient to ensure state integrity.

A notable exception is the scheduler. While the scheduler itself is run in a synchronized dispatcher, request assignments
are sent to the elevators via the event bus. It is possible for the scheduler to assign multiple requests to the same elevator
before the elevator can accept any of the assignments. While the state of the elevator's trajectory will still be consistent,
the later assignments are made with stale state. While the requests will be fulfilled, this could be globally suboptimal.
To remedy this, the elevator can reject assignments based on stale data and trigger rescheduling. This is essentially optimistic
locking to avoid nested pessimistic locks across objects.

### Simulation Drivers
The simulation classes process the event queue of the bus and periodically fire clock tick events.
It should be possible to use multiple concurrent event loop workers with little modification.

`OfflineSimulator` processes events in a single thread with no delay up until a predefined clock limit. This is useful for some
integration tests. Since it is not concurrent, it interleaves firing clock ticks with event processing.

`FixedRateSimulator` uses a `ScheduledExecutorService` to fire clock ticks periodically while running a blocking event loop.
You can use `startAsync` to run it in a background thread. The simulator runs indefinitely and can be terminated by calling the
shutdown method to send a termination signal to the event loop.

### Domain Model
The objects of the domain model are aggregated into the `Building` class which is composed of floors and elevators. In addition
it contains references to the event bus and scheduler.

The return-to-home behavior is decoupled from the scheduler to allow it to be enabled or disabled independent of scheduler implementation.
The scheduler will can still account for this by examining `endTime`, `timeLeftOnTask` or `timeUntilIdle` depending on what behavior is desired.

- endTime: The absolute clock tick at which its tasks will be completed and the elevator will begin heading home
- timeLeftOnTask: The number of clock ticks from now until all currently assigned tasks are complete
- timeUntilIdle:
    - For homing elevators, this is the clock ticks from now until the elevator will return home if it takes no additional tasks.
    - Standard elevators idle at the last floor they serve so its equivalent to timeLeftOnTask

The `Floor` class is relatively simple. It just contains queues of passengers assigned to each elevator. When an elevator arrives
it will fire a LoadPassenger event for each passenger assigned to that elevator.

The `Elevator` class on the other hand needs to keep track of its passengers and its trajectory. The trajectory is tracked
as a reference to an immutable object that generates its successive states by various methods on its API.
The return-to-home behavior of the elevators is determine by whether it uses a `HomingTrajectory` or base `Trajectory`.
Refactoring this into an `IdleBehavior` strategy object would be a better design.

## Configuration
Configuration is entirely programmatic at the moment. There is no runtime configuration or dependency injection in the implementation.

The tick rate, number of elevators and floors are controlled by constants in class `App`.
Scheduler and homing behavior may be changed by setting alternative values on the Building builder in `App::init`.
The return to home feature can be disabled by replacing the `HomingElevatorFactory` with the standard `ElevatorFactory`.

## Scheduling
Currently the two available schedulers are:

- RRFIFOScheduler: a round-robin scheduler that simply cycles through each elevator in a deterministic fashion
- FlockScheduler: Algorithmically determines which elevator to assign the task to by `timeUntilIdle`

The scheduling criterion is to minimize the remaining time across all elevators in the building until the elevators return home,
assuming that no further requests are made. The scheduler evaluates the possible trajectories for each elevator to fulfill a request
and picks the one with minimum `timeUntilIdle` (time until an elevator returns home).

Not a rigorous proof of anything...

- Given $U$ the set of trajectories currently assigned to the elevators
- Let $A$ be the set of all elevator trajectories augmented by the request to be assigned.
- Let $S$ be the subset of $A$ where $s \in S$ if $timeUntilIdle(s) < max(timeUntilIdle(U))$
- There are two cases:
    - When $S$ is empty, no elevator can fulfil the request without increasing the global timeUntilIdle. The best option is to pick the augmented trajectory with the smallest timeUntilIdle.
    - If $S$ is non-empty, then picking any of these will not affect the global timeUntilIdle. The trajectory with the smallest timeUntilIdle must be a member of this set, so choosing it satisfies the criterion.

## Wish List
- [ ] Coverage report for integration
- [x] Scheduler retry jittering
- [x] Request accept/reject statistics listener
- [ ] Websocket event bridge for web frontend
- [ ] Better unit test coverage - especially on Elevator
- [ ] Factor out return-to-home behavior from HomingTrajectory to a strategy object
- [x] Isolate event streams by topic: use separate worker threads for each topic
