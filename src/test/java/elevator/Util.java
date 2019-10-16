package elevator;

import elevator.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.stream.LongStream;
import java.util.stream.Stream;

public class Util {

    @NotNull
    public static Stream<Event.ClockTick> getClockStream() {
        return LongStream.iterate(0, i -> i + 1).mapToObj(Event.ClockTick::new);
    }
}
