package org.firstinspires.ftc.teamcode.autonomous.paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.pedropathing.api.Paths;
import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.firstinspires.ftc.teamcode.actions.Action;
import org.junit.Test;

public class StallProtectedPathActionTest {
    @Test public void progressResetsWatchdogAndStallingStopsDriveImmediately() {
        AtomicInteger motorStops = new AtomicInteger();
        TestFollower follower = new TestFollower(motorStops);
        follower.holdEnd.set(true);
        AtomicLong elapsedNanos = new AtomicLong();
        StallProtectedPathAction action = new StallProtectedPathAction(follower,
                Paths.line(Pose.zero(), new Pose(7, 0)), elapsedNanos::get);

        action.start();
        assertFalse(follower.holdEnd.get());
        action.update();
        elapsedNanos.set(250_000_000);
        follower.remaining = 6;
        action.update();
        elapsedNanos.set(500_000_000);
        action.update();
        assertEquals(Action.Result.RUNNING, action.result());

        elapsedNanos.set(600_000_000);
        action.update();
        assertEquals(Action.Result.TIMED_OUT, action.result());
        assertFalse(follower.isBusy());
        assertTrue(follower.holdEnd.get());
        assertEquals(1, motorStops.get());
    }

    private static final class TestFollower extends Follower {
        private boolean busy;
        private double remaining = 7;

        TestFollower(AtomicInteger motorStops) {
            super(null, (Drivetrain) Proxy.newProxyInstance(Drivetrain.class.getClassLoader(),
                    new Class<?>[] {Drivetrain.class}, (proxy, method, args) -> {
                        if (method.getName().equals("stop")) motorStops.incrementAndGet();
                        return null;
                    }), null);
        }

        @Override public void follow(Path path) { busy = true; }
        @Override public boolean isBusy() { return busy; }
        @Override public double remainingDistance() { return remaining; }
        @Override public void stop() { busy = false; }
    }
}
