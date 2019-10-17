package elevator.util;

import io.vavr.Function3;
import io.vavr.collection.Iterator;
import io.vavr.collection.Queue;
import io.vavr.control.Option;

public class Splice {
    public static Option<Queue<Integer>> splice(int current, Queue<Integer> points, int start, int end, boolean strict, Function3<Integer, Integer, Integer, Boolean> inRange) {
        int left = current;
        Queue<Integer> result = Queue.empty();
        final Iterator<Integer> it = points.iterator();
        if (it.isEmpty())
            return Option.none();

        int right = it.next();
        while (true) {
            if (inRange.apply(left, start, end)) {
                if (result.isEmpty() || start != left && start != right)
                    result = result.append(start);
                break;
            }

            if (!it.hasNext())
                return Option.none();

            result = result.append(right);
            left = right;
            right = it.next();
        }


        while (true) {
            if (inRange.apply(left, end, right)) {
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

        // Flush remainder and return positive result
        result = result.append(right);

        while (it.hasNext())
            result = result.append(it.next());

        return Option.some(result);
    }

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
