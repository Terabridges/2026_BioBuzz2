package org.firstinspires.ftc.teamcode.control.drive;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.drive.Drive2_0;
import org.firstinspires.ftc.teamcode.subsystems.drive.TotalDrive;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;
import org.firstinspires.ftc.teamcode.utility.driveUtility.Rates;

@Configurable
public class DriveControl implements Control {
    private final TotalDrive drive;
    private final Gamepad gp1;
    private final Gamepad gp2;

    // Edge Detectors
    private final EdgeDetector toggleSlowEdgeDetect;
    private final EdgeDetector toggleFieldCentricEdgeDetect;
    private final EdgeDetector toggleDriveModeEdgeDetect;
    private final EdgeDetector toggleDriveHoldEdgeDetect;

    // State Variables
    private boolean slowMode = false;

    // --------Rate Settings---------
    public static double maxRate = 1.0;
    public static double moveCenterRate = 0.7;
    public static double moveExpo = 0.4;
    public static double deadzone = 0.01;

    public static double turnCenterRate = 0.375;
    public static double turnExpo = 0.4;

    // --------Constructors---------
    public DriveControl(TotalDrive drive, Gamepad gp1, Gamepad gp2) {
        this.drive = drive;
        this.gp1 = gp1;
        this.gp2 = gp2;

        toggleSlowEdgeDetect = new EdgeDetector(this::toggleSlowMode);
        toggleFieldCentricEdgeDetect = new EdgeDetector(drive::toggleFieldCentric);
        toggleDriveModeEdgeDetect = new EdgeDetector(this::toggleDriveMode);
        toggleDriveHoldEdgeDetect = new EdgeDetector(() -> drive.getPedroDrive().toggleIsDriveOrHold());
    }

    // ---------Toggles--------------
    public void toggleSlowMode() {
        slowMode = !slowMode;
    }

    public void toggleDriveMode() {
        if (drive.getMode() == TotalDrive.DriveMode.RAW_TELEOP) {
            drive.setMode(TotalDrive.DriveMode.PEDRO_PATHING);
        } else {
            drive.setMode(TotalDrive.DriveMode.RAW_TELEOP);
        }
    }

    // -------Interface Methods------
    @Override
    public void update() {
        // Update Edge Detectors
        toggleSlowEdgeDetect.update(gp1.b);
        toggleFieldCentricEdgeDetect.update(gp1.x);
        toggleDriveModeEdgeDetect.update(gp1.y);
        toggleDriveHoldEdgeDetect.update(gp1.dpad_down);

        // Raw Stick Inputs
        double rawAxial = -gp1.left_stick_y;
        double rawLateral = gp1.left_stick_x;
        double rawYaw = gp1.right_stick_x;

        // Apply Rates & Exponential Curves
        Rates moveRate = new Rates(moveCenterRate, maxRate, moveExpo, deadzone);
        Rates turnRate = new Rates(turnCenterRate, maxRate, turnExpo, deadzone);

        double axial = moveRate.apply(rawAxial);
        double lateral = moveRate.apply(rawLateral);
        double yaw = turnRate.apply(rawYaw);

        // Apply Slow Mode Multiplier
        if (slowMode) {
            axial *= 0.5;
            lateral *= 0.5;
            yaw *= 0.5;
        }

        // Route Control to the Active Drive Engine
        if (drive.getMode() == TotalDrive.DriveMode.RAW_TELEOP) {
            if (drive.isFieldCentric()) {
                drive.getRawDrive().driveFieldRelative(axial, lateral, yaw);
            } else {
                drive.getRawDrive().drive(axial, lateral, yaw);
            }
        } else if (drive.getMode() == TotalDrive.DriveMode.PEDRO_PATHING) {
            Drive2_0 pedro = drive.getPedroDrive();

            // Override active pathing if driver moves sticks beyond deadzone or if follower finished path
            boolean driverIsMovingSticks = Math.abs(rawAxial) > 0.1 ||
                    Math.abs(rawLateral) > 0.1 ||
                    Math.abs(rawYaw) > 0.1;

            if (driverIsMovingSticks || !pedro.isBusy()) {
                if (!drive.isFieldCentric()) {
                    if (pedro.isDriveOrHold) {
                        pedro.driveOrHoldRobotCentric(axial, lateral, yaw);
                    } else {
                        pedro.robotCentric(axial, lateral, yaw);
                    }
                } else {
                    if (pedro.isDriveOrHold) {
                        pedro.driveOrHoldFieldCentric(axial, lateral, yaw);
                    } else {
                        pedro.fieldCentric(axial, lateral, yaw);
                    }
                }
            }
        }
    }

    @Override
    public void addTelemetry(Telemetry t) {
        t.addData("Drive Mode", drive.getMode());
        t.addData("Slow Mode", slowMode);
        t.addData("Field Centric", drive.isFieldCentric());
        t.addData("Pedro DriveOrHold", drive.getPedroDrive().isDriveOrHold);
        t.addData("Pedro Pose", drive.getPedroDrive().robotPose());
    }
}