package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import static org.junit.Assert.*;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import java.lang.reflect.Proxy;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoConfig;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoContext;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoRoutine;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.junit.Test;

/** Computer tests for lifecycle ownership and cleanup, without initializing robot hardware. */
public class BaseAutoOpModeTest {
    private static final class TestFollower extends Follower {
        int updates;
        int stops;
        Pose pose = Pose.zero();

        TestFollower() { super(null, null, null); }
        @Override public void update() { updates++; }
        @Override public void stop() { stops++; }
        @Override public void setPose(Pose pose) { this.pose = pose; }
        @Override public Pose pose() { return pose; }
    }

    private static final class TestRoutine implements AutoRoutine {
        Result result = Result.READY;
        int updates;
        int stops;
        boolean finishOnUpdate;
        RuntimeException startFailure;
        RuntimeException updateFailure;
        RuntimeException stopFailure;

        @Override public void start() {
            result = Result.RUNNING;
            if (startFailure != null) throw startFailure;
        }
        @Override public void update() {
            updates++;
            if (updateFailure != null) throw updateFailure;
            if (finishOnUpdate) result = Result.SUCCEEDED;
        }
        @Override public void stop() {
            stops++;
            if (!isFinished()) result = Result.CANCELLED;
            if (stopFailure != null) throw stopFailure;
        }
        @Override public Result result() { return result; }
        @Override public String state() { return result.name(); }
    }

    private static final class TestAuto extends BaseAutoOpMode {
        final AutoConfig config = new AutoConfig("Test", Pose.zero());
        TestFollower follower;
        TestRoutine nextRoutine = new TestRoutine();
        AutoContext createdContext;

        TestAuto() {
            telemetry = (Telemetry) Proxy.newProxyInstance(Telemetry.class.getClassLoader(),
                    new Class<?>[]{Telemetry.class}, (proxy, method, args) -> {
                        if (method.getReturnType() == boolean.class) return false;
                        return null;
                    });
        }
        @Override protected AutoContext.Mode executionMode() { return AutoContext.Mode.PATH_TEST; }
        // Even if a subclass requests Robot, PATH_TEST must never construct it.
        @Override protected boolean usesRobot() { return true; }
        @Override protected Robot createRobot() {
            throw new AssertionError("Path tests must not construct Robot");
        }
        @Override protected Follower createFollower() {
            follower = new TestFollower();
            return follower;
        }
        @Override protected AutoConfig selectedConfig() { return config; }
        @Override protected AutoRoutine createRoutine(AutoContext context) {
            createdContext = context;
            return nextRoutine;
        }
    }

    @Test public void initDoesNotStartRoutineAndLoopUpdatesFollowerExactlyOnce() {
        TestAuto auto = new TestAuto();
        auto.init();
        auto.init_loop();
        assertNull(auto.createdContext);
        assertEquals(AutoRoutine.Result.READY, auto.nextRoutine.result());
        auto.start();
        assertEquals(AutoContext.Mode.PATH_TEST, auto.createdContext.mode);
        int before = auto.follower.updates;
        auto.loop();
        assertEquals(before + 1, auto.follower.updates);
        assertEquals(1, auto.nextRoutine.updates);
    }

    @Test public void completionCleansUpOnceAndStopsFurtherUpdates() {
        TestAuto auto = new TestAuto();
        auto.nextRoutine.finishOnUpdate = true;
        auto.init();
        auto.start();
        auto.loop();
        assertEquals(AutoRoutine.Result.SUCCEEDED, auto.nextRoutine.result());
        assertEquals(1, auto.nextRoutine.stops);
        int followerStops = auto.follower.stops;
        int followerUpdates = auto.follower.updates;
        auto.stop();
        auto.loop();
        assertEquals(1, auto.nextRoutine.stops);
        assertEquals(followerStops, auto.follower.stops);
        assertEquals(followerUpdates, auto.follower.updates);
    }

    @Test public void updateFailureStillStopsFollowerIfRoutineCleanupAlsoFails() {
        TestAuto auto = new TestAuto();
        RuntimeException original = new IllegalStateException("Update failed");
        RuntimeException cleanup = new IllegalStateException("Cleanup failed");
        auto.nextRoutine.updateFailure = original;
        auto.nextRoutine.stopFailure = cleanup;
        auto.init();
        auto.start();
        int before = auto.follower.stops;
        try {
            auto.loop();
            fail("Routine failure must be reported");
        } catch (RuntimeException failure) {
            assertSame(original, failure);
            assertArrayEquals(new Throwable[]{cleanup}, failure.getSuppressed());
        }
        assertEquals(before + 1, auto.follower.stops);
        assertEquals(1, auto.nextRoutine.stops);
    }

    @Test public void startFailureCleansUpBeforeReportingFailure() {
        TestAuto auto = new TestAuto();
        RuntimeException original = new IllegalStateException("Start failed");
        auto.nextRoutine.startFailure = original;
        auto.init();
        int before = auto.follower.stops;
        try {
            auto.start();
            fail("Routine failure must be reported");
        } catch (RuntimeException failure) {
            assertSame(original, failure);
        }
        assertEquals(before + 1, auto.follower.stops);
        assertEquals(1, auto.nextRoutine.stops);
    }

    @Test public void reinitializationStopsOldRunAndStartsWithFreshRoutineState() {
        TestAuto auto = new TestAuto();
        auto.init();
        auto.start();
        TestRoutine oldRoutine = auto.nextRoutine;
        TestFollower oldFollower = auto.follower;
        auto.nextRoutine = new TestRoutine();
        auto.init();
        assertEquals(1, oldRoutine.stops);
        assertEquals(AutoRoutine.Result.CANCELLED, oldRoutine.result());
        assertNotSame(oldFollower, auto.follower);
        auto.init_loop();
        assertEquals(0, auto.nextRoutine.updates);
        auto.start();
        auto.loop();
        assertEquals(1, auto.nextRoutine.updates);
    }
}
