package org.firstinspires.ftc.teamcode.autonomous.framework;

import static org.junit.Assert.*;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import org.firstinspires.ftc.teamcode.actions.Action;
import org.junit.Test;

/**
 * Computer-run unit tests, not a Driver Station OpMode. Verify that PATH_TEST never
 * constructs a real mechanism action and COMPETITION reports missing Robot hardware.
 * These test mode behavior, not physical movement.
 */
public class AutoContextTest {
    private AutoContext context(AutoContext.Mode mode) {
        return new AutoContext(new AutoConfig("Test", Pose.zero()),
                new Follower(null, null, null), null, mode);
    }

    @Test public void pathTestNeverConstructsOrRunsMechanismAction() {
        Action action = context(AutoContext.Mode.PATH_TEST).mechanismAction(robot -> {
            fail("Path testing must not access Robot or construct a real mechanism action");
            return null;
        });
        assertFalse(action.isFinished());
        action.start();
        assertTrue(action.isFinished());
        action.update();
        action.stop();
        assertTrue(action.isFinished());
    }

    @Test public void competitionReportsMissingRobotInsteadOfSkippingActions() {
        try {
            context(AutoContext.Mode.COMPETITION).mechanismAction(robot -> {
                fail("Factory should not run without Robot");
                return null;
            });
            fail("Missing Robot should be reported");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("requiresRobot"));
        }
    }
}
