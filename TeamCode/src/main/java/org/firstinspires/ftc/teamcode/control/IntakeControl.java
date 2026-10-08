package org.firstinspires.ftc.teamcode.control;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.subsystems.Drive;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;

public class IntakeControl implements Control{
    Intake intake;
    Gamepad gp1;
    Gamepad gp2;
    Robot robot;

    public IntakeControl(Intake intake, Gamepad gp1, Gamepad gp2){
        this.intake = intake;
        this.gp1 = gp1;
        this.gp2 = gp2;
    }

    public IntakeControl(Robot robot, Gamepad gp1, Gamepad gp2) {
        this.robot = robot;
        this.intake = robot.intake;
        this.gp1 = gp1;
        this.gp2 = gp2;
    }

    @Override
    public void update() {
        intake.update();

        if (gp1.right_trigger > 0.1) {
            intake.setMegaSpinIn();
        } else if (gp1.left_trigger > 0.1) {
            intake.setMegaSpinOut();
        } else {
            intake.setMegaSpinZero();
        }
    }

    @Override
    public void addTelemetry(Telemetry t) {

    }
}
