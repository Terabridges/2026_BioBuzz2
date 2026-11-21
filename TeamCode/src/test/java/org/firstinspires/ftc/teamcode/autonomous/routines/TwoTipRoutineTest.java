package org.firstinspires.ftc.teamcode.autonomous.routines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoCatalog;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoContext;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoRoutine;
import org.firstinspires.ftc.teamcode.autonomous.framework.StartSide;
import org.firstinspires.ftc.teamcode.autonomous.paths.FieldPoses;
import org.junit.Test;

public class TwoTipRoutineTest {
    private final AutoCatalog.Entry entry = AutoCatalog.ENTRIES.stream()
            .filter(candidate -> candidate.config.name.equals("Two Tip"))
            .findFirst().orElseThrow(AssertionError::new);
    private final TestFollower follower = new TestFollower();
    private final AtomicBoolean aDown = new AtomicBoolean();

    @Test public void registeredMechanismRouteCanBeDrivenWithoutHardwareInPathTest() {
        assertEquals(Alliance.RED, entry.config.alliance);
        assertEquals(StartSide.AUDIENCE, entry.config.startSide);
        assertEquals(FieldPoses.RED_AUDIENCE_START, entry.config.startPose);
        assertTrue(entry.requiresRobot);
        AutoRoutine routine = createRoutine(AutoContext.Mode.PATH_TEST);
        routine.start();
        updateWithButton(routine, true);
        assertEquals(1, follower.paths.size());

        for (int step = 0; step < 12 && !routine.isFinished(); step++) {
            follower.busy = false;
            updateWithButton(routine, false);
            updateWithButton(routine, true);
        }
        assertEquals(4, follower.paths.size());
        assertEquals(1, follower.turnCommands);
        assertEquals(AutoRoutine.Result.SUCCEEDED, routine.result());

        assertFlowerPose(follower.paths.get(1).endPose(), 119, -159.3045);
        assertFlowerPose(follower.pose(), 119, 90);
        assertFlowerPose(follower.paths.get(2).get(0), 119, 90);
        assertFlowerPose(follower.paths.get(2).endPose(), 127.875, 90);
        assertFlowerPose(follower.paths.get(3).get(0), 127.875, 90);
    }

    @Test public void pathTestWaitsForFreshPressAndDiscardsPressesDuringMovement() {
        aDown.set(true);
        AutoRoutine routine = createRoutine(AutoContext.Mode.PATH_TEST);
        routine.start();
        routine.update();
        assertEquals(0, follower.paths.size());

        updateWithButton(routine, false);
        updateWithButton(routine, true);
        assertEquals(1, follower.paths.size());
        updateWithButton(routine, false);
        updateWithButton(routine, true);
        updateWithButton(routine, false);
        follower.busy = false;
        updateWithButton(routine, true);
        routine.update();
        assertEquals(1, follower.paths.size());
        assertTrue(routine.state().contains("Press A to advance"));
        assertEquals(1, follower.stopCommands);

        updateWithButton(routine, false);
        updateWithButton(routine, true);
        assertEquals(2, follower.paths.size());
    }

    private static void assertFlowerPose(Pose pose, double y, double headingDegrees) {
        assertEquals(48.0, pose.x(), 1e-6);
        assertEquals(y, pose.y(), 1e-6);
        double headingError = Math.IEEEremainder(Math.toDegrees(pose.heading()) - headingDegrees, 360);
        assertEquals(0, headingError, 1e-6);
    }

    private AutoRoutine createRoutine(AutoContext.Mode mode) {
        return entry.create(new AutoContext(entry.config, follower, null, mode, aDown::get));
    }

    private void updateWithButton(AutoRoutine routine, boolean pressed) {
        aDown.set(pressed);
        routine.update();
    }

    private static final class TestFollower extends Follower {
        private final List<Path> paths = new ArrayList<>();
        private int turnCommands;
        private int stopCommands;
        private boolean busy;
        private Pose pose = Pose.zero();

        TestFollower() {
            super(null, (Drivetrain) Proxy.newProxyInstance(Drivetrain.class.getClassLoader(),
                    new Class<?>[] {Drivetrain.class}, (proxy, method, args) -> null), null);
        }
        @Override public void follow(Path path) { paths.add(path); busy = true; }
        @Override public void hold(Pose pose) { turnCommands++; this.pose = pose; }
        @Override public Pose pose() { return pose; }
        @Override public boolean isBusy() { return busy; }
        @Override public void stop() { stopCommands++; busy = false; }
    }
}
