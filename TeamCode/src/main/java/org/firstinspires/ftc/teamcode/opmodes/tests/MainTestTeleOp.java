package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.pedropathing.follower.Follower;
import com.pedropathing.ivy.Scheduler;
import com.pedropathing.math.Pose;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.bylazar.telemetry.JoinedTelemetry;
import com.bylazar.telemetry.PanelsTelemetry;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.control.DriveControl;
import org.firstinspires.ftc.teamcode.control.SpinnerControl;
import org.firstinspires.ftc.teamcode.control.VisionControl;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.psilynx.psikit.core.Logger;
import org.psilynx.psikit.core.wpi.math.Pose2d;
import org.psilynx.psikit.core.wpi.math.Rotation2d;
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@PsiKitAutoLog(rlogPort = 5802)
@TeleOp
public class MainTestTeleOp extends OpMode {
    Follower follower;
    GoBildaPinpointDriver pinpoint;
    Robot robot;
    private JoinedTelemetry joinedTelemetry;

    Gamepad currentGamepad1;
    Gamepad previousGamepad1;

    Gamepad currentGamepad2;
    Gamepad previousGamepad2;
    VisionControl visionControl;
    SpinnerControl intakeControl;
    DriveControl driveControl;
    List<Control> controls;

    public ElapsedTime telemetryTimer;

    @Override
    public void init() {
        List<LynxModule> allHubs = hardwareMap.getAll(LynxModule.class);
        for (LynxModule hub : allHubs) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }
        pinpoint = hardwareMap.get(GoBildaPinpointDriver.class, "pinpoint");

        robot = new Robot(hardwareMap, telemetry, gamepad1, gamepad2);
        follower = robot.follower;
        Scheduler.reset();
        visionControl = new VisionControl(robot, gamepad1, gamepad2);
        intakeControl = new SpinnerControl(robot, gamepad1, gamepad2);
        driveControl = new DriveControl(robot, gamepad1, gamepad2);

        controls = new ArrayList<>(Arrays.asList(visionControl, intakeControl, driveControl));

        currentGamepad1 = new Gamepad();
        previousGamepad1 = new Gamepad();

        currentGamepad2 = new Gamepad();
        previousGamepad2 = new Gamepad();

        follower.setPose(new Pose(0,0,0));

        joinedTelemetry = new JoinedTelemetry(
                PanelsTelemetry.INSTANCE.getFtcTelemetry(),
                telemetry
        );
        telemetryTimer = new ElapsedTime();
    }

    @Override
    public void init_loop() {
        previousGamepad1.copy(currentGamepad1);
        currentGamepad1.copy(gamepad1);
        telemetry.update();
    }

    @Override
    public void start() {
        robot.toInit();
        telemetryTimer.reset();
    }

    @Override
    public void loop() {
        follower.update();
        Scheduler.execute();
        gamepadUpdate();
        controlsUpdate();
        robot.update();
        controlsTelemetryUpdate();
    }

    @Override
    public void stop() {
        robot.stop();
    }

    public void controlsUpdate() {
        for (Control c: controls) {
            c.update();
        }
    }

    public void controlsTelemetryUpdate() {
        if (telemetryTimer.milliseconds() > 200) {
            for (Control c : controls) {
                c.addTelemetry(joinedTelemetry);
            }
            joinedTelemetry.addData("Follower Busy", follower.isBusy());
            joinedTelemetry.addData("Robot X", follower.pose().x());
            joinedTelemetry.addData("Robot Y", follower.pose().y());
            joinedTelemetry.addData("Heading", follower.pose().heading());
            joinedTelemetry.update();
            telemetryTimer.reset();
            Logger.recordOutput("Robot Pose", new Pose2d(follower.pose().x(), follower.pose().y(), Rotation2d.fromRadians(follower.pose().heading())));
            Pose targetPose = robot.vision.getBestClusterPose(follower.pose());
            Logger.recordOutput("Target Pose", new Pose2d(targetPose.x(), targetPose.y(), Rotation2d.fromRadians(targetPose.heading())));
        }
    }

    public void gamepadUpdate(){
        previousGamepad1.copy(currentGamepad1);
        currentGamepad1.copy(gamepad1);

        previousGamepad2.copy(currentGamepad2);
        currentGamepad2.copy(gamepad2);
    }

}
