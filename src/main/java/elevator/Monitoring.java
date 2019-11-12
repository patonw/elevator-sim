package elevator;

import elevator.event.RunnableEventBus;
import elevator.event.SynchronizedEventBus;
import elevator.simulation.WatchdogReactor;
import io.vavr.control.Try;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.threads.Pauser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Monitoring {
    private static Logger log = LoggerFactory.getLogger("monitor.elevator.Monitoring"); // meh
    final static String CHRONICLE_DIR = App.CHRONICLE_DIR;

    private final ChronicleQueue queue;
    private final ExcerptTailer tailer;
    private final MethodReader reader;
    private final RunnableEventBus bus;

    public Monitoring() {
        queue = ChronicleQueue.singleBuilder(CHRONICLE_DIR).build();
        tailer = queue.createTailer("Monitor");

        // Streaming is receive-only. Events fired through this bus will not be sent to simulation process.
        bus = new SynchronizedEventBus();
        bus.attach(new LoggingEventListener(log));
        bus.attach(new WatchdogReactor());

        reader = tailer.methodReader(bus);
    }

    public void monitor() throws ExecutionException, InterruptedException {
        AtomicBoolean shutdown = new AtomicBoolean(false);
        final CompletableFuture<Void> runner = CompletableFuture.runAsync(() -> Try.run(() -> bus.run(shutdown)));
        Pauser pauser = Pauser.millis(0,100);
        while(!reader.isClosed()) {
            if (reader.readOne())
                pauser.reset();
            else
                pauser.pause();
        }

        shutdown.set(true);
        runner.get();
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final Monitoring monitoring = new Monitoring();
        monitoring.monitor();
    }
}
