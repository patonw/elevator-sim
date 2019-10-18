package elevator.event;

import java.util.concurrent.Semaphore;

public interface RunnableEventBus extends EventBus {
    /**
     * Broadcasts all queued items to reactors and returns when the queue is empty.
     *
     * Event reactors can fire additional events while handling queued events.
     * {process} will not return until all these follow-up events have been handled.
     *
     * This can be used in offline simulation or to trigger processing in asynchronous event
     * emitters in the absence of a daemon thread.
     *
     * @return Number of events processed in this iteration.
     */
    int process();

    /**
     * Wait for new events and process them on arrival.
     *
     * Exits once the shutdownSig is released.
     *
     * @param shutdownSig A semaphore that triggers shutdown when released
     */
    void run(Semaphore shutdownSig) throws InterruptedException;
}
