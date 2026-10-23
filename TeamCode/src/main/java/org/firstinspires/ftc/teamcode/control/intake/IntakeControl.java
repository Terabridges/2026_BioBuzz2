package org.firstinspires.ftc.teamcode.control.intake;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.intake.Ramp;
import org.firstinspires.ftc.teamcode.subsystems.intake.Spinner;
import org.firstinspires.ftc.teamcode.subsystems.intake.Transfer;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;

public class IntakeControl implements Control {

    //---------------- Software ----------------
    Intake intake;
    Ramp ramp;
    Spinner spinner;
    Transfer transfer;
    Gamepad gp1;
    Gamepad gp2;

    EdgeDetector spinInRE = new EdgeDetector(() -> spinner.setSpinnerSpinIn());
    EdgeDetector spinInFE = new EdgeDetector(() -> spinner.setSpinnerSpinZero(), true);
    EdgeDetector spinOutRE = new EdgeDetector(() -> spinner.setSpinnerSpinOut());
    EdgeDetector spinOutFE = new EdgeDetector(() -> spinner.setSpinnerSpinZero(), true);

    //---------------- Constructor ----------------
    public IntakeControl(Intake intake, Gamepad gp1, Gamepad gp2){
        this.intake = intake;
        this.ramp = intake.ramp;
        this.spinner = intake.spinner;
        this.transfer = intake.transfer;
        this.gp1 = gp1;
        this.gp2 = gp2;
    }

    //---------------- Methods ----------------
    private void updateRamp(){

    }

    private void updateSpinner(){
        spinInRE.update(gp1.right_trigger > 0.1);
        spinInFE.update(gp1.right_trigger > 0.1);
        spinOutRE.update(gp1.left_trigger > 0.1);
        spinOutFE.update(gp1.left_trigger > 0.1);
    }

    private void updateTransfer(){

    }

    //---------------- Interface Methods ----------------
    @Override
    public void update(){
        updateRamp();
        updateSpinner();
        updateTransfer();
    }

    @Override
    public void addTelemetry(Telemetry telemetry){

    }
}