package org.firstinspires.ftc.teamcode.control.Intake;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.subsystems.intake.Spinner;
import org.firstinspires.ftc.teamcode.util.EdgeDetector;

public class SpinnerControl implements Control {
    Spinner spinner;
    Gamepad gp1;
    Gamepad gp2;
    Robot robot;

    EdgeDetector spinIn = new EdgeDetector(() -> spinner.setMegaSpinIn());
    EdgeDetector spinOut = new EdgeDetector(() -> spinner.setMegaSpinOut());
    EdgeDetector noSpin = new EdgeDetector(() -> spinner.setMegaSpinZero());
    public SpinnerControl(Spinner spinner, Gamepad gp1, Gamepad gp2) {
        this.spinner = spinner;
        this.gp1 = gp1;
        this.gp2 = gp2;
    }

    public SpinnerControl(Robot robot, Gamepad gp1, Gamepad gp2) {
        this(robot.intake.spinner, gp1, gp2);
        this.robot = robot;
    }

    @Override
    public void update() {
        noSpin.update(gp1.right_trigger < 0.1 && gp1.left_trigger < 0.1);
        spinIn.update(gp1.right_trigger > 0.1);
        spinOut.update(gp1.left_trigger > 0.1);
    }

    @Override
    public void addTelemetry(Telemetry t) {

    }
}
