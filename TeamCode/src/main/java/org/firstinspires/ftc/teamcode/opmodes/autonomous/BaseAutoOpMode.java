package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.autonomous.framework.AutoConfig;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoContext;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoRoutine;
import org.firstinspires.ftc.teamcode.pedro.Constants;
import org.firstinspires.ftc.teamcode.subsystems.Robot;

/**
 * Common FTC lifecycle: initialize hardware/follower, accept pre-start selection,
 * start a fresh routine, update it each loop, and stop on completion or Driver Station Stop.
 * Owns follower/Robot updates and status telemetry, not strategy-specific states or paths.
 * Manual Drive updates are disabled when Robot is present so Pedro owns drive commands.
 * PATH_TEST initializes only the follower, excluding unrelated mechanism hardware.
 */
public abstract class BaseAutoOpMode extends OpMode {
    private Follower follower;
    private Robot robot;
    private AutoConfig config;
    private AutoRoutine routine;
    private boolean stopped;
    private final ElapsedTime autoTimer = new ElapsedTime();

    protected abstract AutoConfig selectedConfig();
    protected abstract AutoRoutine createRoutine(AutoContext context);

    protected AutoContext.Mode executionMode() { return AutoContext.Mode.COMPETITION; }

    /** Path-only diagnostics override this to avoid initializing unrelated hardware. */
    protected boolean usesRobot() { return executionMode() != AutoContext.Mode.PATH_TEST; }
    protected Robot createRobot() { return new Robot(hardwareMap, telemetry); }
    protected Follower createFollower() { return Constants.create(hardwareMap); }
    protected void resetSelection() {}
    protected void updateSelection() {}
    protected void addSelectionTelemetry() {}

    @Override public final void init() {
        if (follower != null) stop();
        stopped = false;
        robot = null;
        follower = null;
        routine = null;
        resetSelection();
        if (executionMode() != AutoContext.Mode.PATH_TEST && usesRobot()) {
            robot = createRobot();
            robot.drive.manualDrive = false;
            robot.toInit();
        }
        // Initialize Pedro after Drive so its motor configuration takes precedence.
        follower = createFollower();
        follower.stop();
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
        follower.update();
        showTelemetry();
    }

    @Override public final void start() {
        // Selection is frozen from here onward; nothing moves in init_loop.
        try {
            follower.setPose(config.startPose);
            routine = createRoutine(new AutoContext(config, follower, robot, executionMode()));
            autoTimer.reset();
            routine.start();
        } catch (RuntimeException failure) {
            stopAfterFailure(failure);
            throw failure;
        }
    }

    @Override public final void loop() {
        if (stopped) return;
        try {
            follower.update();
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
                if (follower != null) follower.stop();
            } finally {
                if (robot != null) robot.stop();
            }
        }
    }

    private void showTelemetry() {
        telemetry.addData("Auto", config.selectionLabel());
        telemetry.addData("Mode", executionMode());
        telemetry.addData("State", routine == null ? "Ready" : routine.state());
        telemetry.addData("Result", routine == null ? AutoRoutine.Result.READY : routine.result());
        telemetry.addData("Elapsed (s)", routine == null ? 0.0 : autoTimer.seconds());
        Pose pose = follower.pose();
        telemetry.addData("Pose (in, deg)", "%.1f, %.1f, %.1f",
                pose.x(), pose.y(), Math.toDegrees(pose.heading()));
        if (routine == null) addSelectionTelemetry();
        telemetry.update();
    }
}
