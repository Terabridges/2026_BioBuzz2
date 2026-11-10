package org.firstinspires.ftc.teamcode.control.drive;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.drive.PedroDrive;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;

/** Manual driving and optional position hold for IntakeDriveHoldTest. */
public class DriveHoldTestControl implements Control {
    private final PedroDrive drive;
    private final Gamepad gp1;
    private boolean robotCentric;
    private boolean driveOrHold = true;
    private final EdgeDetector centricity = new EdgeDetector(() -> robotCentric = !robotCentric);
    private final EdgeDetector driveHold = new EdgeDetector(() -> driveOrHold = !driveOrHold);

    public DriveHoldTestControl(PedroDrive drive, Gamepad gp1) {
        this.drive = drive;
        this.gp1 = gp1;
    }

    @Override
    public void update() {
        centricity.update(gp1.dpad_up);
        driveHold.update(gp1.dpad_down);

        if (robotCentric) {
            if (driveOrHold) {
                drive.driveOrHoldRobotCentric(-gp1.left_stick_y, gp1.left_stick_x, gp1.right_stick_x);
            } else {
                drive.robotCentric(-gp1.left_stick_y, gp1.left_stick_x, gp1.right_stick_x);
            }
        } else {
            if (driveOrHold) {
                drive.driveOrHoldFieldCentric(-gp1.left_stick_y, gp1.left_stick_x, gp1.right_stick_x);
            } else {
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
