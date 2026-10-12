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
        toggleSlowEdgeDetect.update(gp1.b);
        toggleFieldCentricEdgeDetect.update(gp1.x);

        double axial = -gp1.left_stick_y;  // Note: pushing stick forward gives negative value
        double lateral = gp1.left_stick_x;
        double yaw = gp1.right_stick_x;

        Rates moveRate = new Rates(moveCenterRate, maxRate, moveExpo, deadzone);
        Rates turnRate = new Rates(turnCenterRate, maxRate, turnExpo, deadzone);

        // Convert axial, lateral, and yaw from stickInputs to powerRequests using the rates we just built
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
        } else if(drive.manualDrive) {
            // POV Mode uses left joystick to go forward & strafe, and right joystick to rotate.
            drive.drive(axial, lateral, yaw);
        }
    }

    @Override
    public void addTelemetry(Telemetry t) {
        t.addData("Slow Mode", slowMode);
        t.addData("Field Centric", drive.useFieldCentric);
    }
}
