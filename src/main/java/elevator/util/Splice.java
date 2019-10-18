package elevator.util;

import io.vavr.Function3;
import io.vavr.collection.Iterator;
import io.vavr.collection.Queue;
import io.vavr.control.Option;

public class Splice {
    private static Option<Queue<Integer>> splice(int current, Queue<Integer> points, int start, int end, boolean strict, Function3<Integer, Integer, Integer, Boolean> isMonotonic) {
        // Too many concurrency issues with accepting a request from the current floor
        if (current == start)
            return Option.none();

        int left = current;
        Queue<Integer> result = Queue.empty();
        final Iterator<Integer> it = points.iterator();
        if (it.isEmpty())
            return Option.none();

        int right = it.next();

        // Process the turnpoints up to the insertion of [start]
        while (true) {
            if (isMonotonic.apply(left, start, right) && result.nonEmpty()) {
                if (start != left && start != right)
                    result = result.append(start);
                break;
            }

            // End of turnpoints and nowhere to insert [start] -> fail
            if (!it.hasNext())
                return Option.none();

            result = result.append(right);
            left = right;
            right = it.next();
        }


        // Process the turnpoints up to the insertion of [end]
        while (true) {
            if (isMonotonic.apply(left, end, right)) {
                if (end != left && end != right)
                    result = result.append(end);
                break;
            }

            if (!it.hasNext()) {
                if (strict)
                    return Option.none();

                // Turnpoints do not include end, but appending it will result in a shorter path than appending both start and end
                result = result.append(right).append(end);

                return Option.some(result);
            }

            result = result.append(right);
            left = right;
            right = it.next();
        }

        // Flush remainder and return spliced result
        result = result.append(right);

        while (it.hasNext())
            result = result.append(it.next());

        return Option.some(result);
    }

    /**
     * Inserts the span defined by [start,end] into the trajectory and returns the result as a new Queue.
     *
     * In non-strict mode, if the start falls within the current trajectory, it will append end to the result.
     * This is shorter than backtracking to start before continuing to end.
     *
     * @param current The current floor
     * @param points Existing turnpoints in the trajectory
     * @param start The beginning of the span to inject
     * @param end The end of the span to inject
     * @param strict Forces splicing to abort if [start,end] cannot be wholly contained by points
     * @return Either None or a new Queue representing the result
     */
    public static Option<Queue<Integer>> splice(int current, Queue<Integer> points, int start, int end, boolean strict) {
        if (start < end)
            return splice(current, points, start, end, strict, (x,y,z) -> x <= y && y <= z);
        else
            return splice(current, points, start, end, strict, (x,y,z) -> x >= y && y >= z);
    }

    public static Option<Queue<Integer>> splice(int current, Queue<Integer> points, int start, int end) {
        return splice(current, points, start, end, false);
    }

}
