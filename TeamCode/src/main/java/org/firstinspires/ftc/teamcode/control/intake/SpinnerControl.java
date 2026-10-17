package org.firstinspires.ftc.teamcode.control.intake;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.intake.Spinner;
import org.firstinspires.ftc.teamcode.util.EdgeDetector;

public class SpinnerControl implements Control {

    //---------------- Software ----------------
    Spinner spinner;
    Gamepad gp1;
    Gamepad gp2;
    Robot robot;

    EdgeDetector spinInRE = new EdgeDetector(() -> spinner.setSpinnerSpinIn());
    EdgeDetector spinInFE = new EdgeDetector(() -> spinner.setSpinnerSpinZero(), true);
    EdgeDetector spinOutRE = new EdgeDetector(() -> spinner.setSpinnerSpinOut());
    EdgeDetector spinOutFE = new EdgeDetector(() -> spinner.setSpinnerSpinZero(), true);

    //---------------- Constructor ----------------
    public SpinnerControl(Intake intake, Gamepad gp1, Gamepad gp2){
        this.spinner = intake.spinner;
        this.gp1 = gp1;
        this.gp2 = gp2;
    }

    public SpinnerControl(Robot robot, Gamepad gp1, Gamepad gp2) {
        this(robot.intake, gp1, gp2);
        this.robot = robot;
    }

    //---------------- Methods ----------------

    //---------------- Interface Methods ----------------
    @Override
    public void update(){
        spinInRE.update(gp1.right_trigger > 0.1);
        spinInFE.update(gp1.right_trigger > 0.1);
        spinOutRE.update(gp1.left_trigger > 0.1);
        spinOutFE.update(gp1.left_trigger > 0.1);
    }

    @Override
    public void addTelemetry(Telemetry telemetry){

    }
}
