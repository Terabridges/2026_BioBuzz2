package org.firstinspires.ftc.teamcode.control;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.subsystems.Drive;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.util.EdgeDetector;
import org.firstinspires.ftc.teamcode.util.Rates;

@Configurable
public class DriveControl implements Control{
    private Drive drive;
    private Gamepad gp1;
    private Gamepad gp2;
    private Robot robot;
    private EdgeDetector toggleSlowEdgeDetect;
    private EdgeDetector toggleFieldCentricEdgeDetect;
    private boolean slowMode = false;

    //--------Rate Settings---------
    public static double maxRate = 1.0;
    public static double moveCenterRate = 0.7;
    public static double moveExpo = 0.4;
    public static double deadzone = 0.01;

    public static double turnCenterRate = 0.375;
    public static double turnExpo = 0.4;

    //--------Constructor---------
    public DriveControl(Drive drive, Gamepad gp1, Gamepad gp2){
        this.drive = drive;
        this.gp1 = gp1;
        this.gp2 = gp2;
    }

    public DriveControl(Robot robot, Gamepad gp1, Gamepad gp2) {
        this.robot = robot;
        this.drive = robot.drive;
        this.gp1 = gp1;
        this.gp2 = gp2;

        // TODO: Since slowMode and fieldCentric are things that this class cares about, we should bring both into this class
        toggleSlowEdgeDetect = new EdgeDetector(this::toggleSlowMode);
        toggleFieldCentricEdgeDetect = new EdgeDetector(drive::toggleFieldCentric);
    }

    //---------Methods--------------
    public void toggleSlowMode() {
        slowMode = !slowMode;
    }

    //-------Interface Methods------
    @Override
    public void update() {
        toggleSlowEdgeDetect.update(gp1.b); // Note: gp1.b is also bound to driveToPose in VisionControl
        toggleFieldCentricEdgeDetect.update(gp1.x);

        boolean isAutoActive = !robot.follower.idle() ||
                (robot.activeAutoCommand != null && robot.activeAutoCommand.isScheduled());

        // 1. Driver stick override cancels auto immediately
        if (isAutoActive) {
            boolean driverOverride = Math.abs(gp1.left_stick_y) > 0.1 ||
                    Math.abs(gp1.left_stick_x) > 0.1 ||
                    Math.abs(gp1.right_stick_x) > 0.1;

            if (driverOverride) {
                if (robot.activeAutoCommand != null) {
                    robot.activeAutoCommand.cancel();
                    robot.activeAutoCommand = null;
                }

                robot.follower.stop(); // Transitions Follower to IDLE (follower.idle() == true)
                drive.stop();
                robot.intake.spinner.setMegaSpinZero();

                isAutoActive = false;
            }
        }

        drive.manualDrive = !isAutoActive;

        // 2. Manual driving (Follower is guaranteed to be IDLE here)
        if (drive.manualDrive) {
            if (!robot.follower.idle()) {
                robot.follower.stop();
            }

            double axial = -gp1.left_stick_y;
            double lateral = gp1.left_stick_x;
            double yaw = gp1.right_stick_x;

            Rates moveRate = new Rates(moveCenterRate, maxRate, moveExpo, deadzone);
            Rates turnRate = new Rates(turnCenterRate, maxRate, turnExpo, deadzone);

            axial = moveRate.apply(axial);
            lateral = moveRate.apply(lateral);
            yaw = turnRate.apply(yaw);

            if (slowMode) {
                axial *= 0.5;
                lateral *= 0.5;
                yaw *= 0.5;
            }

            if (drive.useFieldCentric) {
                drive.driveFieldRelative(axial, lateral, yaw);
            } else {
                drive.drive(axial, lateral, yaw);
            }
        }
    }

    @Override
    public void addTelemetry(Telemetry t) {
        t.addData("Slow Mode", slowMode);
        t.addData("Field Centric", drive.useFieldCentric);
    }
}
