package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.autonomous.framework.AutoConfig;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoContext;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoRoutine;
import org.firstinspires.ftc.teamcode.pedro.Constants;
import org.firstinspires.ftc.teamcode.pedro.FollowerManager;
import org.firstinspires.ftc.teamcode.subsystems.Robot;

/**
 * Common FTC lifecycle: initialize hardware/follower, accept pre-start selection,
 * start a fresh routine, update it each loop, and stop on completion or Driver Station Stop.
 * Owns manager/Robot updates and status telemetry, not strategy-specific states or paths.
 * Robot owns mechanisms; only FollowerManager updates the drivetrain follower.
 * A started competition auto offers its stopped follower to the next TeleOp.
 * Init-only runs, failed runs, and path tests never publish a handoff.
 * PATH_TEST initializes only the follower, excluding unrelated mechanism hardware.
 */
public abstract class BaseAutoOpMode extends OpMode {
    private Follower follower;
    private FollowerManager followerManager;
    private Robot robot;
    private AutoConfig config;
    private AutoRoutine routine;
    private boolean stopped;
    private boolean started;
    private boolean failed;
    private final ElapsedTime autoTimer = new ElapsedTime();
    private long lastLoopStartNs;

    protected abstract AutoConfig selectedConfig();
    protected abstract AutoRoutine createRoutine(AutoContext context);

    protected AutoContext.Mode executionMode() { return AutoContext.Mode.COMPETITION; }
    protected boolean pathTestAdvanceDown() { return false; }

    /** Path-only diagnostics override this to avoid initializing unrelated hardware. */
    protected boolean usesRobot() { return executionMode() != AutoContext.Mode.PATH_TEST; }
    protected Robot createRobot() { return new Robot(hardwareMap, telemetry); }
    protected Follower createFollower() { return Constants.create(hardwareMap); }
    protected void resetSelection() {}
    protected void updateSelection() {}
    protected void addSelectionTelemetry() {
        telemetry.addData("SELECTED AUTO", config.selectionLabel());
    }

    @Override public final void init() {
        if (follower != null) stop();
        stopped = false;
        started = false;
        failed = false;
        robot = null;
        follower = null;
        followerManager = null;
        routine = null;
        FollowerManager.clearHandoff();
        resetSelection();
        follower = createFollower();
        followerManager = new FollowerManager(hardwareMap, follower);
        config = selectedConfig();
        follower.setPose(config.startPose);
        showTelemetry();
    }

    @Override public final void init_loop() {
        updateSelection();
        AutoConfig selected = selectedConfig();
        if (selected != config) {
            config = selected;
            follower.setPose(config.startPose);
        }
        showTelemetry();
    }

    @Override public final void start() {
        // Selection is frozen from here onward; nothing moves in init_loop.
        try {
            if (executionMode() != AutoContext.Mode.PATH_TEST && usesRobot()) {
                robot = createRobot();
                robot.outtake.setMotionSource(follower::pose, follower::velocity);
                robot.toInit();
            }
            follower.setPose(config.startPose);
            routine = createRoutine(new AutoContext(
                    config, follower, robot, executionMode(), this::pathTestAdvanceDown));
            autoTimer.reset();
            lastLoopStartNs = 0;
            routine.start();
            started = true;
        } catch (RuntimeException failure) {
            stopAfterFailure(failure);
            throw failure;
        }
    }

    @Override public final void loop() {
        if (stopped) return;
        long now = System.nanoTime();
        if (lastLoopStartNs != 0) {
            telemetry.addData("Loop time (ms)", "%.2f", (now - lastLoopStartNs) / 1_000_000.0);
        }
        lastLoopStartNs = now;
        try {
            followerManager.update();
            if (routine != null && !routine.isFinished()) routine.update();
            if (routine != null && routine.isFinished()) {
                stop();
            } else if (robot != null) {
                robot.update();
            }
            showTelemetry();
        } catch (RuntimeException failure) {
            stopAfterFailure(failure);
            throw failure;
        }
    }

    private void stopAfterFailure(RuntimeException failure) {
        failed = true;
        FollowerManager.clearHandoff();
        try {
            stop();
        } catch (RuntimeException cleanupFailure) {
            // Keep the original failure visible even if cleanup also fails.
            if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
        }
    }

    @Override public final void stop() {
        if (stopped) return;
        stopped = true;
        try {
            if (routine != null) routine.stop();
        } finally {
            try {
                if (robot != null) robot.stop();
            } finally {
                if (followerManager != null) followerManager.stop();
            }
        }
        if (started && !failed && config.allowPoseHandoff && executionMode() == AutoContext.Mode.COMPETITION) {
            followerManager.handoffToTeleOp();
        }
    }

    private void showTelemetry() {
        if (routine == null) {
            addSelectionTelemetry();
            telemetry.update();
            return;
        }
        telemetry.addData("SELECTED AUTO", config.selectionLabel());
        telemetry.addData("Mode", executionMode());
        telemetry.addData("State", routine.state());
        telemetry.addData("Result", routine.result());
        telemetry.addData("Elapsed (s)", autoTimer.seconds());
        Pose pose = follower.pose();
        telemetry.addData("Pose (in, deg)", "%.1f, %.1f, %.1f",
                pose.x(), pose.y(), Math.toDegrees(pose.heading()));
        telemetry.update();
    }
}
