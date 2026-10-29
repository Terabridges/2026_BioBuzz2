package org.firstinspires.ftc.teamcode.control.drive;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.drive.PedroDrive;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;

/** Specialized hold/path-override controls for the predictive tracking test only. */
public class PredictiveDriveControl implements Control {
    PedroDrive drive;
    Gamepad gp1;
    Gamepad gp2;
    private boolean robotCentric;
    private boolean driveOrHold = true;
    EdgeDetector centricity = new EdgeDetector(() -> robotCentric = !robotCentric);
    EdgeDetector driveHold = new EdgeDetector(() -> driveOrHold = !driveOrHold);

    public PredictiveDriveControl(PedroDrive drive, Gamepad gp1, Gamepad gp2) {
        this.drive = drive;
        this.gp1 = gp1;
        this.gp2 = gp2;
    }

    @Override
    public void update() {
        centricity.update(gp1.dpad_up);
        driveHold.update(gp1.dpad_down);

        boolean driverIsMovingSticks = Math.abs(gp1.left_stick_y) > 0.1 ||
                Math.abs(gp1.left_stick_x) > 0.1 ||
                Math.abs(gp1.right_stick_x) > 0.1;

        if (driverIsMovingSticks || !drive.isBusy()) {
            if (robotCentric) {

                if (driveOrHold) {
                    drive.driveOrHoldRobotCentric(-gp1.left_stick_y, gp1.left_stick_x, gp1.right_stick_x);
                } else
                    drive.robotCentric(-gp1.left_stick_y, gp1.left_stick_x, gp1.right_stick_x);

            } else {
                if (driveOrHold) {
                    drive.driveOrHoldFieldCentric(-gp1.left_stick_y, gp1.left_stick_x, gp1.right_stick_x);
                } else
                    drive.fieldCentric(-gp1.left_stick_y, gp1.left_stick_x, gp1.right_stick_x);

            }
        }
    }

    @Override
    public void addTelemetry(Telemetry t) {
        t.addData("RobotCentric", robotCentric);
        t.addData("DriveOrHold", driveOrHold);
        t.addData("Pose", drive.robotPose());
    }
}
