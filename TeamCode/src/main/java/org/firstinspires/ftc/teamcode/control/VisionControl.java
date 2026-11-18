package org.firstinspires.ftc.teamcode.control;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.Scheduler;
import static com.pedropathing.ivy.Scheduler.schedule;
import static com.pedropathing.ivy.commands.Commands.instant;
import static com.pedropathing.ivy.commands.Commands.waitMs;
import static com.pedropathing.ivy.commands.Commands.waitUntil;
import static com.pedropathing.ivy.pedro.PedroCommands.follow;

import com.pedropathing.ivy.groups.Groups;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.pedro.Constants;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.subsystems.vision.Cluster;
import org.firstinspires.ftc.teamcode.subsystems.vision.DetectedBall;
import org.firstinspires.ftc.teamcode.subsystems.vision.Vision;
import org.firstinspires.ftc.teamcode.util.EdgeDetector;
import org.psilynx.psikit.core.Logger;

public class VisionControl implements Control {
    Vision vision;
    Gamepad gp1;
    Gamepad gp2;
    Robot robot;

    private final EdgeDetector snapButton;
    private final EdgeDetector driveToPose;
    private final EdgeDetector testPose;

    private final double OVERSHOOT_DISTANCE = 8.0; // Aim 8 inches PAST the ball to keep speed up

    // Assuming a standard 0 to 144 inch coordinate system.
    // If your code uses -72 to 72, change these to ~ -62 and 62.
    private final double FIELD_MIN_X = 10.0;
    private final double FIELD_MAX_X = 134.0;
    private final double FIELD_MIN_Y = 10.0;
    private final double FIELD_MAX_Y = 134.0;
    private Pose lastTargetPose = null;
    private final double RECALCULATE_THRESHOLD = 2.0;

    // --- NEW: Flag to tell the update loop when to actively track ---
    private boolean isAutoFetching = false;

    public VisionControl(Vision vision, Gamepad gp1, Gamepad gp2)
    {
        this.vision = vision;
        this.gp1 = gp1;
        this.gp2 = gp2;

        this.snapButton = new EdgeDetector(vision::takeSnapshot);
        this.driveToPose = new EdgeDetector(() -> {
            if (!robot.follower.isBusy()) {
                Path path = vision.driveToBestCluster(this.robot.follower.pose(), vision.getBestClusterPose(this.robot.follower.pose()));
                this.robot.follower.follow(path);
            }
        });
        this.testPose = new EdgeDetector(() -> robot.follower.follow(line(robot.follower.pose(), new Pose(10,10, Math.PI / 2)).linear(robot.follower.pose(),new Pose(10,10, Math.PI / 2) )));
    }

    public VisionControl(Robot robot, Gamepad gp1, Gamepad gp2)
    {
        this.robot = robot;
        this.vision = robot.vision;
        this.gp1 = gp1;
        this.gp2 = gp2;

        this.snapButton = new EdgeDetector(vision::takeSnapshot);
        this.driveToPose = new EdgeDetector(() -> {
            boolean autoRunning = !this.robot.follower.idle() ||
                    (this.robot.activeAutoCommand != null && this.robot.activeAutoCommand.isScheduled());

            if (!autoRunning) {
                // --- NEW: Reset tracking variables and turn the flag ON ---
                isAutoFetching = true;
                stopTracking();

                Pose target = vision.getBestClusterPose(this.robot.follower.pose(), 0);
                Path path = vision.driveToBestCluster(this.robot.follower.pose(), target);

                // Trigger radius (in inches from target pose) to start spinning the intake
                double INTAKE_TRIGGER_RADIUS_INCHES = 4.0;

                // --- FIXED: Check distance to the actively updating lastTargetPose ---
                Command waitUntilNearBall = waitUntil(() -> {
                    if (lastTargetPose == null) return false;
                    Pose current = this.robot.follower.pose();
                    return Math.hypot(lastTargetPose.x() - current.x(), lastTargetPose.y() - current.y()) <= INTAKE_TRIGGER_RADIUS_INCHES;
                });

                this.robot.activeAutoCommand = Groups.sequential(
                        Groups.race(
                                follow(this.robot.follower, path),

                                // --- FIXED: The Success Sequence ---
                                // Wait until we get close, turn on the intake, wait 750ms to suck it in.
                                // When this sequence finishes, it wins the race and stops the follower!
                                Groups.sequential(
                                        waitUntilNearBall,
                                        instant(() -> robot.intake.spinner.setMegaSpinIn()),
                                        waitMs(750)
                                ),

                                waitMs(3500) // Fallback timeout so it doesn't get stuck forever
                        ),

                        // Once the race is won (or times out), clean everything up
                        instant(() -> {
                            robot.intake.spinner.setMegaSpinZero();
                            this.robot.follower.manual(); // Kills PedroPathing momentum
                            isAutoFetching = false;
                            stopTracking();
                        })
                );

                this.robot.activeAutoCommand.schedule();
            }
        });
        this.testPose = new EdgeDetector(() -> this.robot.follower.follow(line(this.robot.follower.pose(), new Pose(10,10, Math.PI / 2)).linear(this.robot.follower.pose(),new Pose(10,10, Math.PI / 2) )));

    }

    @Override
    public void update() {
        snapButton.update(gp1.a);
        driveToPose.update(gp1.y);
        testPose.update(gp1.dpad_up);

        // --- NEW: Manual Override Detection ---
        // Check if the driver is pushing any of the drive sticks past a 10% deadzone
        boolean driverIntervention = Math.abs(gp1.left_stick_y) > 0.1 ||
                Math.abs(gp1.left_stick_x) > 0.1 ||
                Math.abs(gp1.right_stick_x) > 0.1;

        if (isAutoFetching) {

            // If the driver touches the sticks, KILL the auto-fetch sequence immediately
            if (driverIntervention) {
                isAutoFetching = false;
                stopTracking();
                this.robot.follower.manual(); // Instantly breaks the PedroPathing sequence

                // If Ivy allows cancelling commands directly, it's good practice to clear it so it doesn't resume
                Scheduler.cancel(this.robot.activeAutoCommand); // Uncomment if Ivy supports this
            }
            else {
                // Driver is hands-off, continue hunting the pollen
                trackMovingPollen(this.robot.follower, vision);

                // Safety fallback: if command finishes naturally
                if (this.robot.activeAutoCommand != null && !this.robot.activeAutoCommand.isScheduled()) {
                    isAutoFetching = false;
                    stopTracking();
                }
            }
        }
    }

    @Override
    public void addTelemetry(Telemetry t) {
//        t.addData("Network: ", vision.getConnectionStatus());
//        t.addData("Snapshots Saved ", vision.getSnapshotCount());
        t.addData("Target Pose", vision.getBestClusterPose(robot.follower.pose()));
        t.addData("Detected balls", new Cluster().getBalls());
        t.addData("Has Target?", vision.hasTarget());
        t.addData("Object(s) Tx:", vision.getTx());
        t.addData("Objects(s) Ty: ", vision.getTy());
        t.addData("Object(s) Area:", vision.getArea());
        t.addData("Is Auto Fetching?", isAutoFetching);
    }

    public void trackMovingPollen(Follower follower, Vision vision) {
        Pose rawTarget = vision.getBestClusterPose(follower.pose(), 0.0);

        if (rawTarget.x() != 0 && rawTarget.y() != 0) {

            // 1. Target Jump Check (Did we eat the ball?)
            if (lastTargetPose != null) {
                double targetJumpDistance = Math.hypot(
                        rawTarget.x() - lastTargetPose.x(),
                        rawTarget.y() - lastTargetPose.y()
                );
                if (targetJumpDistance > 15.0) {
                    return;
                }
            }

            // 2. Commit Distance
            double distanceToTarget = Math.hypot(
                    rawTarget.x() - follower.pose().x(),
                    rawTarget.y() - follower.pose().y()
            );

            if (distanceToTarget < 15.0) {
                return;
            }

            // 3. Noise Threshold
            boolean shouldUpdatePath = false;
            if (lastTargetPose == null) {
                shouldUpdatePath = true;
            } else {
                double distMoved = Math.hypot(
                        rawTarget.x() - lastTargetPose.x(),
                        rawTarget.y() - lastTargetPose.y()
                );
                if (distMoved > 4.0) {
                    shouldUpdatePath = true;
                }
            }

            // 4. Calculate Overshoot and apply Wall Boundaries
            if (shouldUpdatePath) {
                // Find the angle straight through the ball
                double angleToBall = Math.atan2(
                        rawTarget.y() - follower.pose().y(),
                        rawTarget.x() - follower.pose().x()
                );

                // Push the coordinate past the ball so we don't decelerate early
                double overshotX = rawTarget.x() + (Math.cos(angleToBall) * OVERSHOOT_DISTANCE);
                double overshotY = rawTarget.y() + (Math.sin(angleToBall) * OVERSHOOT_DISTANCE);

                // Clamp the coordinates so it NEVER routes a path into the wall
                double safeX = Math.max(FIELD_MIN_X, Math.min(FIELD_MAX_X, overshotX));
                double safeY = Math.max(FIELD_MIN_Y, Math.min(FIELD_MAX_Y, overshotY));

                // Build the new safe pose, keeping the original heading
                Pose safeTarget = new Pose(safeX, safeY, rawTarget.heading());

                // Feed the safe, clamped, overshot target to PedroPathing
                Path safePath = vision.driveToBestCluster(follower.pose(), safeTarget);
                follower.follow(safePath);

                // IMPORTANT: Save the raw target, not the fake overshot one,
                // so our math on the next loop isn't messed up!
                lastTargetPose = rawTarget;
            }
        }
    }

    public void stopTracking() {
        lastTargetPose = null; // Reset so it calculates immediately next time
    }
}