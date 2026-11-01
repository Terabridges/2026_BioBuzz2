package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.bylazar.telemetry.JoinedTelemetry;
import com.bylazar.telemetry.PanelsTelemetry;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.control.drive.DriveHoldTestControl;
import org.firstinspires.ftc.teamcode.subsystems.drive.PedroDrive;
import org.firstinspires.ftc.teamcode.control.intake.IntakeControl;
import org.firstinspires.ftc.teamcode.control.vision.VisionControl;
import org.firstinspires.ftc.teamcode.pedro.FollowerManager;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.utility.driveUtility.PoseLogging;
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@PsiKitAutoLog(rlogPort = 5802)
@TeleOp(name = "Intake Drive Hold Test", group = "Tests")
public class IntakeDriveHoldTest extends OpMode {
    Follower follower;
    FollowerManager followerManager;
    Robot robot;
    private JoinedTelemetry joinedTelemetry;

    VisionControl visionControl;
    IntakeControl intakeControl;
    DriveHoldTestControl driveControl;
    List<Control> controls;

    public ElapsedTime telemetryTimer;

    @Override
    public void init() {
        followerManager = FollowerManager.forTeleOp(hardwareMap);
        follower = followerManager.follower();
        robot = new Robot(hardwareMap, telemetry);
        visionControl = new VisionControl(robot.vision, gamepad1, gamepad2);
        intakeControl = new IntakeControl(robot.intake, gamepad1, gamepad2);
        driveControl = new DriveHoldTestControl(new PedroDrive(follower), gamepad1);

        controls = new ArrayList<>(Arrays.asList(visionControl, intakeControl, driveControl));
        if (!followerManager.resumedFromAuto()) {
            follower.setPose(new Pose(8.262, 8.309, Math.toRadians(90)));
        }

        joinedTelemetry = new JoinedTelemetry(
                PanelsTelemetry.INSTANCE.getFtcTelemetry(),
                telemetry
        );
        telemetryTimer = new ElapsedTime();
    }

    @Override
    public void init_loop() {
        followerManager.update();
        telemetry.addData("Localization", followerManager.resumedFromAuto() ? "From autonomous" : "Fresh follower");
        telemetry.update();
    }

    @Override
    public void start() {
        robot.toInit();
        followerManager.startManual();
        telemetryTimer.reset();
    }

    @Override
    public void loop() {
        controlsUpdate();
        robot.update();

        followerManager.update();
        controlsTelemetryUpdate();
        logPsiKitData();
        robot.logPsiKitData();
    }

    @Override
    public void stop() {
        try {
            if (robot != null) robot.stop();
        } finally {
            if (followerManager != null) followerManager.stop();
        }
    }

    public void logPsiKitData() {
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
            joinedTelemetry.update();
            telemetryTimer.reset();
        }
    }

}
