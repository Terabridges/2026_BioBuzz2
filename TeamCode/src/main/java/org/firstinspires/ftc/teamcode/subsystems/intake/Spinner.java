package org.firstinspires.ftc.teamcode.subsystems.intake;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.util.Utility;

public class Spinner implements Subsystem {

    //---------------- Hardware ----------------
    private final DcMotorEx spinner;

    //---------------- Software ----------------
    private double spinnerPow = 0.0;


    //---------------- Constructor ----------------
    public Spinner(HardwareMap map) {
        spinner = map.get(DcMotorEx.class, "spinner");
    }

    //---------------- Core Methods ----------------
    private void setSpinner(double pow){
        spinner.setPower(pow);
    }

    //---------------- Methods ----------------
    public void setSpinnerPow(double pow){
        spinnerPow = pow;
    }

    public void setSpinnerSpinIn() {
        spinnerPow = 0.98;
    }

    public void setSpinnerSpinOut() {
        spinnerPow = -0.98;
    }

    public void setSpinnerSpinZero() {
        spinnerPow = 0.0;
    }

    //---------------- Interface Methods ----------------
    @Override
    public void init(){}

    @Override
    public void update(){
        setSpinner(spinnerPow);
    }


}
