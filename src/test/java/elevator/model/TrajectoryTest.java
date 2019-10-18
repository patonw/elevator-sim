package elevator.model;

import elevator.util.Splice;
import io.vavr.collection.List;
import io.vavr.collection.Queue;
import io.vavr.control.Option;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class TrajectoryTest {
    @Test
    public void testIdleTrajectory() {
        Trajectory first = new Trajectory(0,0);

        assertThat(first.getEndTime(), is(0L));
        assertThat(first.getEndFloor(), is(0));
        assertThat(first.nextFloor(), is(0));

        Trajectory second = new Trajectory(10,42);

        assertThat(second.getEndTime(), is(10L));
        assertThat(second.getEndFloor(), is(42));
        assertThat(second.nextFloor(), is(42));
    }

    @Test
    public void testUnitTrajectory() {
        Trajectory first = new Trajectory(0,0).extend(0,1);
        Trajectory second = first.extend(1,0);

        assertThat(first.getEndTime(), is(1L));
        assertThat(first.getEndFloor(), is(1));
        assertThat(first.nextFloor(), is(1));
        assertTrue(first.shouldStop());

        assertThat(second.getEndTime(), is(2L));
        assertThat(second.getEndFloor(), is(0));
        assertThat(second.nextFloor(), is(1));
    }

    @Test public void testDisjoint() {
        Trajectory start = new Trajectory(100,20);

        Trajectory first = start.extend(10, 5);
        Trajectory second = first.extend(20, 31);

        assertThat(first.getEndTime(), is(115L));
        assertThat(first.getEndFloor(), is(5));
        assertThat(first.nextFloor(), is(19));

        assertThat(second.getEndTime(), is(141L));
        assertThat(second.getEndFloor(), is(31));
        assertThat(second.nextFloor(), is(19));
    }

    @Test
    public void testAugmentPath() {
        Trajectory start = new Trajectory(100,20);

        Trajectory first = start.augment(10, 5);
        Trajectory disjoint = first.augment(7, 22);

        assertThat(first.getEndTime(), is(115L));
        assertThat(first.getEndFloor(), is(5));
        assertThat(first.nextFloor(), is(19));

        assertThat(disjoint.getEndTime(), is(132L));
        assertThat(disjoint.getEndFloor(), is(22));
        assertThat(disjoint.nextFloor(), is(19));

        // Should continue directly from 22 to 27.
        // Should not backtrack to 15 before continuing to 27
        Trajectory overlap = disjoint.augment(15, 27);
        assertThat(overlap.getEndTime(), is(137L));
        assertThat(overlap.getEndFloor(), is(27));
        assertThat(overlap.nextFloor(), is(19));
    }

    @Test
    public void testStepping() {
        Trajectory start = new Trajectory(100,20)
                .extend(10, 5)
                .extend(20, 31);

        Trajectory current = start.step();
        assertThat(current.getCurrentFloor(), is(19));
        assertThat(current.getCurrentTime(), is(101L));
        assertThat(current.getTurnpoints(), is(equalTo(List.of(10, 5, 20, 31).toJavaList())));

        assertThat(current.getEndTime(), is(141L));
        assertThat(current.getEndFloor(), is(31));
        assertThat(current.nextFloor(), is(18));

        current = current.step();
        assertThat(current.getCurrentFloor(), is(18));
        assertThat(current.getCurrentTime(), is(102L));
        assertFalse(current.shouldStop());

        // Fast forward to first turnpoint
        for (int i=0; i<8; i++)
            current = current.step();

        assertThat(current.getCurrentFloor(), is(10));
        assertThat(current.getCurrentTime(), is(110L));
        assertTrue(current.shouldStop());
        // Turnpoint is not removed until we leave it
        assertThat(current.getTurnpoints(), is(equalTo(List.of(10, 5, 20, 31).toJavaList())));

        current = current.step();
        assertThat(current.getCurrentFloor(), is(9));
        assertThat(current.getCurrentTime(), is(111L));
        assertThat(current.getTurnpoints(), is(equalTo(List.of(5, 20, 31).toJavaList())));
        assertFalse(current.shouldStop());

        for (int i=0; i<4; i++)
            current = current.step();

        assertThat(current.getCurrentFloor(), is(5));
        assertThat(current.getCurrentTime(), is(115L));
        assertThat(current.getTurnpoints(), is(equalTo(List.of(5, 20, 31).toJavaList())));
        assertTrue(current.shouldStop());

        for (int i=0; i<20; i++)
            current = current.step();

        assertThat(current.getCurrentFloor(), is(25));
        assertThat(current.getCurrentTime(), is(135L));
        assertThat(current.getTurnpoints(), is(equalTo(List.of(31).toJavaList())));


        // Fast-forward to last turnpoint
        for (int i=0; i<6; i++)
            current = current.step();

        assertThat(current.getCurrentFloor(), is(31));
        assertThat(current.getCurrentTime(), is(141L));
        assertThat(current.getTurnpoints(), is(equalTo(List.of(31).toJavaList())));

        // Continue past last turnpoint by idling in-place
        for (int i=0; i<9; i++)
            current = current.step();

        assertThat(current.getCurrentFloor(), is(31));
        assertThat(current.getCurrentTime(), is(150L));
        assertThat(current.getTurnpoints(), is(equalTo(List.empty().toJavaList())));
    }

    @Test
    public void testInclusion() {
        Trajectory trajectory = new Trajectory(100,20)
                .extend(10, 5)
                .extend(20, 31);

        assertTrue(trajectory.includes(10, 5));
        assertTrue(trajectory.includes(5,10));
        assertFalse(trajectory.includes(9, 2));
        assertTrue(trajectory.includes(5,30));
        assertFalse(trajectory.includes(30, 20));
    }


    @Test public void testSplicing() {
        Queue<Integer> points = Queue.of(44, 65, 90, 91);

        final Option<Queue<Integer>> result = Splice.splice(79, points, 82, 94);
        assertTrue(result.isDefined());
        final Queue<Integer> updated = result.get();

        assertThat(updated, is(Queue.of(44, 65, 82, 90, 91, 94)));

        assertFalse(Splice.splice(79, points, 33, 94).isDefined());
        assertFalse(Splice.splice(79, points, 33, 21).isDefined());
        assertFalse(Splice.splice(79, points, 5, 10).isDefined());

        assertThat(Splice.splice(79, points, 45, 82).get(), is(Queue.of(44, 45, 65, 82, 90, 91)));
    }
}


