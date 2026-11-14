package org.firstinspires.ftc.teamcode.control;

import static com.pedropathing.api.Paths.line;

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
                Pose target = vision.getBestClusterPose(this.robot.follower.pose(), 0);
                Path path = vision.driveToBestCluster(this.robot.follower.pose(), target);

                // Trigger radius (in inches from target pose) to start spinning the intake
                double INTAKE_TRIGGER_RADIUS_INCHES = 8.0;

                Command waitUntilNearBall = waitUntil(() -> {
                    Pose current = this.robot.follower.pose();
                    return Math.hypot(target.x() - current.x(), target.y() - current.y()) <= INTAKE_TRIGGER_RADIUS_INCHES;
                });

                Command runIntake = Command.build()
                        .setStart(() -> robot.intake.spinner.setMegaSpinIn())
                        .setEnd(endCondition -> robot.intake.spinner.setMegaSpinZero());

                this.robot.activeAutoCommand = Groups.sequential(
                        Groups.race(
                                follow(this.robot.follower, path),
                                Groups.sequential(waitUntilNearBall, runIntake),
                                waitMs(2500)
                        ),
                        instant(() -> robot.intake.spinner.setMegaSpinZero()),
                        instant(() -> this.robot.follower.stop())
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
    }
}
