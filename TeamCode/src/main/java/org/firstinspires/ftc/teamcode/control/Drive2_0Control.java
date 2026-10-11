package org.firstinspires.ftc.teamcode.control;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.subsystems.Other.Drive2_0;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.subsystems.vision.Vision;
import org.firstinspires.ftc.teamcode.util.EdgeDetector;

public class Drive2_0Control implements Control{
    Drive2_0 drive20;
    Gamepad gp1;
    Gamepad gp2;
    Robot robot;
    EdgeDetector centricity = new EdgeDetector(() -> drive20.toggleCentricity());
    EdgeDetector driveHold = new EdgeDetector(() -> drive20.toggleIsDriveOrHold());

    public Drive2_0Control(Drive2_0 drive20, Gamepad gp1, Gamepad gp2) {
        this.drive20 = drive20;
        this.gp1 = gp1;
        this.gp2 = gp2;
    }

    public Drive2_0Control(Robot robot, Gamepad gp1, Gamepad gp2) {
        this(robot.drive20, gp1, gp2);
        this.robot = robot;
    }

    @Override
    public void update() {
        centricity.update(gp1.dpad_up);
        driveHold.update(gp1.dpad_down);

        boolean driverIsMovingSticks = Math.abs(gp1.left_stick_y) > 0.1 ||
                Math.abs(gp1.left_stick_x) > 0.1 ||
                Math.abs(gp1.right_stick_x) > 0.1;

        if (driverIsMovingSticks || !drive20.isBusy()) {
            if (drive20.isRobotCentric) {

                if (drive20.isDriveOrHold) {
                    drive20.driveOrHoldRobotCentric(-gp1.left_stick_y, -gp1.left_stick_x, -gp1.right_stick_x);
                } else
                    drive20.robotCentric(-gp1.left_stick_y, -gp1.left_stick_x, -gp1.right_stick_x);

            } else {
                if (drive20.isDriveOrHold) {
                    drive20.driveOrHoldFieldCentric(-gp1.left_stick_y, -gp1.left_stick_x, -gp1.right_stick_x);
                } else
                    drive20.fieldCentric(-gp1.left_stick_y, -gp1.left_stick_x, -gp1.right_stick_x);

            }
        }
    }

    @Override
    public void addTelemetry(Telemetry t) {
        t.addData("RobotCentric", drive20.isRobotCentric);
        t.addData("DriveOrHold", drive20.isDriveOrHold);
        t.addData("Pose", drive20.robotPose());
    }
}
