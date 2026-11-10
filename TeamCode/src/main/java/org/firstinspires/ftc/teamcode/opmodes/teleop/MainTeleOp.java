package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.bylazar.telemetry.JoinedTelemetry;
import com.bylazar.telemetry.PanelsTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.control.drive.DriveControl;
import org.firstinspires.ftc.teamcode.subsystems.drive.DriveManager;
import org.firstinspires.ftc.teamcode.control.intake.IntakeControl;
import org.firstinspires.ftc.teamcode.control.outtake.OuttakeControl;
import org.firstinspires.ftc.teamcode.control.vision.VisionControl;
import org.firstinspires.ftc.teamcode.subsystems.Robot;

@TeleOp(name = "MainTeleOp", group = "Main")
public class MainTeleOp extends OpMode {
    // MainTeleOp drive configuration.
    private static final DriveManager.Backend DRIVE_BACKEND = DriveManager.Backend.PEDRO;
    private static final boolean USE_RATES = true;
    private static final boolean START_FIELD_CENTRIC = false;

    private Robot robot;
    private DriveManager drive;
    private DriveControl driveControl;
    private Control[] mechanismControls;
    private JoinedTelemetry joinedTelemetry;
    private long lastLoop, lastTelemetry;
    private double maxLoopMs;
    private double maxMechanismsMs;
    private double maxDriveMs;

    @Override public void init() {
        drive = new DriveManager(DRIVE_BACKEND);
        robot = new Robot(hardwareMap, telemetry);
        mechanismControls = new Control[]{
                new VisionControl(robot.vision, gamepad1, gamepad2),
                new IntakeControl(robot.intake, gamepad1, gamepad2),
                new OuttakeControl(robot.outtake, gamepad1, gamepad2)};
        joinedTelemetry = new JoinedTelemetry(PanelsTelemetry.INSTANCE.getFtcTelemetry(), telemetry);
        drive.addTelemetry(telemetry);
        telemetry.addData("Rates", USE_RATES);
        telemetry.addData("Field Centric", START_FIELD_CENTRIC);
        telemetry.update();
    }

    @Override public void init_loop() {
        drive.addTelemetry(telemetry);
        telemetry.addData("Rates", USE_RATES);
        telemetry.addData("Field Centric", START_FIELD_CENTRIC);
        telemetry.update();
    }

    @Override public void start() {
        try {
            robot.toInit();
            drive.start(hardwareMap, true);
            driveControl = new DriveControl(drive.drive(), gamepad1, USE_RATES, START_FIELD_CENTRIC);
        } catch (RuntimeException failure) {
            stop();
            throw failure;
        }
        lastLoop = lastTelemetry = System.nanoTime();
        maxLoopMs = 0;
        maxMechanismsMs = 0;
        maxDriveMs = 0;
    }

    @Override public void loop() {
        long now = System.nanoTime();
        double loopMs = (now - lastLoop) / 1e6;
        lastLoop = now;
        maxLoopMs = Math.max(maxLoopMs, loopMs);
        try {
            // Apply driving before potentially slow mechanism/camera hardware reads.
            long driveStart = System.nanoTime();
            driveControl.update();
            drive.update();
            double driveMs = (System.nanoTime() - driveStart) / 1e6;
            maxDriveMs = Math.max(maxDriveMs, driveMs);
            long mechanismsStart = System.nanoTime();
            for (Control control : mechanismControls) control.update();
            robot.update();
            double mechanismsMs = (System.nanoTime() - mechanismsStart) / 1e6;
            maxMechanismsMs = Math.max(maxMechanismsMs, mechanismsMs);
            if (now - lastTelemetry >= 200_000_000L) {
                drive.addTelemetry(joinedTelemetry);
                driveControl.addTelemetry(joinedTelemetry);
                joinedTelemetry.addData("Drive work / max (ms)", "%.1f / %.1f", driveMs, maxDriveMs);
                for (Control control : mechanismControls) control.addTelemetry(joinedTelemetry);
                joinedTelemetry.addData("Loop / max (ms)", "%.1f / %.1f", loopMs, maxLoopMs);
                joinedTelemetry.addData("Mechanisms + vision / max (ms)", "%.1f / %.1f",
                        mechanismsMs, maxMechanismsMs);
                joinedTelemetry.update();
                lastTelemetry = now;
            }
        } catch (RuntimeException failure) {
            stop();
            throw failure;
        }
    }

    @Override public void stop() {
        try {
            if (robot != null) robot.stop();
        } finally {
            if (drive != null) drive.stop();
        }
    }
}
