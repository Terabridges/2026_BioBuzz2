package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.bylazar.telemetry.JoinedTelemetry;
import com.bylazar.telemetry.PanelsTelemetry;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.control.drive.DriveControl;
import org.firstinspires.ftc.teamcode.control.intake.IntakeControl;
import org.firstinspires.ftc.teamcode.control.outtake.OuttakeControl;
import org.firstinspires.ftc.teamcode.control.vision.VisionControl;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.pedro.FollowerManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@TeleOp(name = "MainTeleOp", group = "Main")
public class MainTeleOp extends OpMode {

    //---------------- Objects ----------------
    private GoBildaPinpointDriver pinpoint;
    private Robot robot;
    private FollowerManager followerManager;
    private JoinedTelemetry joinedTelemetry;

    private Gamepad currentGamepad1;
    private Gamepad previousGamepad1;
    private Gamepad currentGamepad2;
    private Gamepad previousGamepad2;

    private ElapsedTime telemetryTimer;

    //---------------- Controls ----------------
    private VisionControl visionControl;
    private IntakeControl intakeControl;
    private OuttakeControl outtakeControl;
    private DriveControl driveControl;
    private List<Control> controls;


    //---------------- init/ basically constructor ----------------
    @Override
    public void init() {
        pinpoint = hardwareMap.get(GoBildaPinpointDriver.class, "pinpoint");

        followerManager = FollowerManager.forTeleOp(hardwareMap);
        robot = new Robot(hardwareMap, telemetry, followerManager.follower());
        visionControl = new VisionControl(robot.vision, gamepad1, gamepad2);
        intakeControl = new IntakeControl(robot.intake, gamepad1, gamepad2);
        outtakeControl = new OuttakeControl(robot.outtake, gamepad1, gamepad2);
        driveControl = new DriveControl(robot.drive20, gamepad1, gamepad2);

        controls = new ArrayList<>(Arrays.asList(
                visionControl,
                intakeControl,
                outtakeControl,
                driveControl));

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

    //---------------- init loop ----------------
    @Override
    public void init_loop() {
        previousGamepad1.copy(currentGamepad1);
        currentGamepad1.copy(gamepad1);
        followerManager.update();
        telemetry.addData("Localization", followerManager.resumedFromAuto() ? "From autonomous" : "Fresh follower");
        telemetry.update();
    }

    //---------------- start ----------------
    @Override
    public void start() {
        robot.toInit();
        followerManager.startManual();
        telemetryTimer.reset();
    }

    //---------------- loop ----------------
    @Override
    public void loop() {
        gamepadUpdate();
        controlsUpdate();
        robot.update();
        followerManager.update();
        controlsTelemetryUpdate();
    }

    //---------------- stop ----------------
    @Override
    public void stop() {
        try {
            if (robot != null) robot.stop();
        } finally {
            if (followerManager != null) followerManager.stop();
        }
    }


    //---------------- Methods ----------------
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

    public void gamepadUpdate(){
        previousGamepad1.copy(currentGamepad1);
        currentGamepad1.copy(gamepad1);

        previousGamepad2.copy(currentGamepad2);
        currentGamepad2.copy(gamepad2);
    }

}
