package elevator.model;

import io.vavr.collection.List;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class HomingTrajectoryTest {
    @Test
    public void testGoHome() {
        Trajectory start = new Trajectory(new IdleToHome(21), 100,20)
                .extend(10, 5)
                .extend(20, 31);
        assertThat(start.timeUntilIdle(), is(51L));

        Trajectory current = start.step();
        assertThat(current.getCurrentFloor(), is(19));
        assertThat(current.getCurrentTime(), is(101L));
        assertThat(current.getTurnpoints(), is(equalTo(List.of(10, 5, 20, 31).toJavaList())));

        assertThat(current.getEndTime(), is(141L));
        assertThat(current.getEndFloor(), is(31));
        assertThat(current.nextFloor(), is(18));

        // Fast forward to first turnpoint
        for (int i=0; i<9; i++)
            current = current.step();

        assertThat(current.getCurrentFloor(), is(10));
        assertThat(current.getCurrentTime(), is(110L));
        assertTrue(current.shouldStop());
        assertThat(current.timeUntilIdle(), is(41L));

        // Fast forward to last turnpoint
        for (int i=0; i<31; i++)
            current = current.step();

        assertThat(current.getCurrentFloor(), is(31));
        assertThat(current.getCurrentTime(), is(141L));
        assertTrue(current.shouldStop());
        assertThat(current.timeUntilIdle(), is(10L));

        assertThat(current.nextFloor(), is(30));
        for (int i=0; i<10; i++)
            current = current.step();

        assertThat(current.getCurrentFloor(), is(21));
        assertThat(current.getCurrentTime(), is(151L));
        for (int i=0; i<10; i++)
            current = current.step();

        assertThat(current.getCurrentFloor(), is(21));
        assertThat(current.getCurrentTime(), is(161L));
        assertThat(current.timeUntilIdle(), is(0L));

        // Check immutability
        assertThat(start.timeUntilIdle(), is(51L));
    }

}
