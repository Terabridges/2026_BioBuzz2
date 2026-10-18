package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.pedropathing.api.Paths;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.bylazar.telemetry.JoinedTelemetry;
import com.bylazar.telemetry.PanelsTelemetry;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.control.drive.Drive2_0Control;
import org.firstinspires.ftc.teamcode.control.intake.IntakeControl;
import org.firstinspires.ftc.teamcode.control.vision.VisionControl;
import org.firstinspires.ftc.teamcode.pedro.FollowerManager;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.subsystems.drive.Drive2_0;
import org.firstinspires.ftc.teamcode.utility.driveUtility.PoseLogging;
import org.psilynx.psikit.core.Logger;
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@PsiKitAutoLog(rlogPort = 5802)
@TeleOp
public class IntakePlusPredictiveTrackingTeleop extends OpMode {
    GoBildaPinpointDriver pinpoint;
    Follower follower;
    Robot robot;
    Drive2_0 drive20;
    private JoinedTelemetry joinedTelemetry;

    Gamepad currentGamepad1;
    Gamepad previousGamepad1;

    Gamepad currentGamepad2;
    Gamepad previousGamepad2;
    VisionControl visionControl;
    IntakeControl intakeControl;
    Drive2_0Control driveControl;
    List<Control> controls;

    public ElapsedTime telemetryTimer;

    @Override
    public void init() {
        pinpoint = hardwareMap.get(GoBildaPinpointDriver.class, "pinpoint");


        robot = new Robot(hardwareMap, telemetry, gamepad1, gamepad2);
        visionControl = new VisionControl(robot.vision, gamepad1, gamepad2);
        intakeControl = new IntakeControl(robot.intake, gamepad1, gamepad2);
        // Robot only runs Drive 1, so this test drives with its own Drive 2.0 and keeps Drive 1 off the motors
        robot.drive.manualDrive = false;
        drive20 = new Drive2_0(hardwareMap);
        driveControl = new Drive2_0Control(drive20, gamepad1, gamepad2);

        controls = new ArrayList<>(Arrays.asList(visionControl, intakeControl, driveControl));
        follower = FollowerManager.getInstance(hardwareMap);
        follower.setPose(new Pose(8.262, 8.309, Math.toRadians(90)));

        currentGamepad1 = new Gamepad();
        previousGamepad1 = new Gamepad();

        currentGamepad2 = new Gamepad();
        previousGamepad2 = new Gamepad();

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
        gamepadUpdate();
        controlsUpdate();
        robot.update();
        drive20.update();

        if ((gamepad1.y && !previousGamepad1.y) && (robot.vision.hasTarget())) {
            Pose targetPose = robot.vision.getPredictiveInterceptPose(follower.pose());
            if (targetPose != null) {
                driveToLocationAuto(targetPose);
            }
        }

        controlsTelemetryUpdate();
        logPsiKitData();
        robot.logPsiKitData();
    }

    @Override
    public void stop() {
        robot.stop();
        drive20.stop();
    }

    public void driveToLocationAuto(Pose targetPose) {
        Pose currentPose = follower.pose();
        Path dynamicLine = Paths.line(currentPose, targetPose).linear(currentPose, targetPose);
        follower.follow(dynamicLine);
    }

    public void logPsiKitData() {
        Logger.recordOutput("TargetPose", PoseLogging.toPose2dFromPedroInchesAsFtcCenterRotatedWithoutHeading(robot.vision.getPredictiveInterceptPose(follower.pose())));
        PoseLogging.logMainPoseDetails(robot, follower);
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
            joinedTelemetry.addData("TargetPose:", robot.vision.getPredictiveInterceptPose(follower.pose()));
            joinedTelemetry.update();
            telemetryTimer.reset();
        }
    }

    public void gamepadUpdate(){
        previousGamepad1.copy(currentGamepad1);
        currentGamepad1.copy(gamepad1);

        previousGamepad2.copy(currentGamepad2);
        currentGamepad2.copy(gamepad2);
    }

}
