package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.teamcode.control.drive.DriveControl;
import org.firstinspires.ftc.teamcode.subsystems.drive.Drive;
import org.firstinspires.ftc.teamcode.subsystems.drive.DriveManager;
import org.firstinspires.ftc.teamcode.pedro.FollowerManager;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;

/** The same drive pipeline as MainTeleOp, with no mechanisms/vision or auto handoff. */
@TeleOp(name = "Drive Comparison Test", group = "Tests")
public class DriveComparisonTest extends OpMode {
    private DriveManager drive;
    private ComparisonDriveControl driveControl;
    private long lastLoop, lastTelemetry;
    private double maxLoopMs;
    private DriveManager.Backend backend = DriveManager.Backend.DIRECT;
    private boolean ratesEnabled;
    private boolean fieldCentric;
    private final EdgeDetector ratesToggle = new EdgeDetector(() -> ratesEnabled = !ratesEnabled);
    private final EdgeDetector initialFieldToggle = new EdgeDetector(() -> fieldCentric = !fieldCentric);
    private double maxDriveMs;

    /** Test mappings are independent of MainTeleOp's DriveControl mappings. */
    private static final class ComparisonDriveControl extends DriveControl {
        private final EdgeDetector fieldToggle = new EdgeDetector(this::toggleFieldCentric);
        private final EdgeDetector slowToggle = new EdgeDetector(this::toggleSlowMode);

        ComparisonDriveControl(Drive drive, Gamepad gamepad, boolean rates, boolean field) {
            super(drive, gamepad, rates, field);
            fieldToggle.isPressed = gamepad.back;
            slowToggle.isPressed = gamepad.b;
        }

        @Override protected void updateButtons() {
            fieldToggle.update(gamepad.back);
            slowToggle.update(gamepad.b);
        }
    }

    @Override public void init() {
        FollowerManager.clearHandoff();
        showSelection();
    }

    @Override public void init_loop() {
        // Test-only drive selection.
        if (gamepad1.dpad_left) backend = DriveManager.Backend.DIRECT;
        else if (gamepad1.dpad_right) backend = DriveManager.Backend.PEDRO;
        ratesToggle.update(gamepad1.x);
        initialFieldToggle.update(gamepad1.back);
        showSelection();
    }

    private void showSelection() {
        telemetry.addData("Drive backend", backend);
        telemetry.addData("Rates", ratesEnabled);
        telemetry.addData("Field Centric", fieldCentric);
        telemetry.addLine("Before Start: D-pad left=Direct/right=Pedro, X=rates, Back=field-centric");
        telemetry.addLine("Driving: B=half speed, Back=field-centric. Stop/Init to change backend/rates.");
        telemetry.update();
    }

    @Override public void start() {
        drive = new DriveManager(backend);
        try {
            drive.start(hardwareMap, false);
            driveControl = new ComparisonDriveControl(drive.drive(), gamepad1, ratesEnabled, fieldCentric);
        } catch (RuntimeException failure) {
            stop();
            throw failure;
        }
        lastLoop = lastTelemetry = System.nanoTime();
        maxLoopMs = 0;
        maxDriveMs = 0;
    }

    @Override public void loop() {
        long now = System.nanoTime();
        double loopMs = (now - lastLoop) / 1e6;
        lastLoop = now;
        maxLoopMs = Math.max(maxLoopMs, loopMs);
        long driveStart = System.nanoTime();
        try {
            driveControl.update();
            drive.update();
        } catch (RuntimeException failure) {
            stop();
            throw failure;
        }
        double driveMs = (System.nanoTime() - driveStart) / 1e6;
        maxDriveMs = Math.max(maxDriveMs, driveMs);
        if (now - lastTelemetry >= 200_000_000L) {
            drive.addTelemetry(telemetry);
            driveControl.addTelemetry(telemetry);
            telemetry.addData("Drive work / max (ms)", "%.1f / %.1f", driveMs, maxDriveMs);
            telemetry.addData("Loop / max (ms)", "%.1f / %.1f", loopMs, maxLoopMs);
            telemetry.update();
            lastTelemetry = now;
        }
    }

    @Override public void stop() { if (drive != null) drive.stop(); }
}
