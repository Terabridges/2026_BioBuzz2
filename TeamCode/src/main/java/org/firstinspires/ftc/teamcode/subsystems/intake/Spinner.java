package org.firstinspires.ftc.teamcode.subsystems.intake;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;

public class Spinner implements Subsystem {

    //---------------- Hardware ----------------
    private DcMotorEx spinner;

    //---------------- Software ----------------
    private double spinnerPow = 0.0;


    //---------------- Constructor ----------------
    public Spinner(HardwareMap map) {
        spinner = map.get(DcMotorEx.class, "spinner");
        spinner.setDirection(DcMotorSimple.Direction.REVERSE);
        spinner.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        spinner.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
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

    public void setSpinnerSpinIn(double pow) {
        spinnerPow = Math.abs(pow);
    }

    public void setSpinnerSpinOut(double pow) {
        spinnerPow = -Math.abs(pow);
    }

    public void setSpinnerSpinZero() {
        spinnerPow = 0.0;
    }

    //---------------- Interface Methods ----------------
    @Override
    public void init(){ stop(); }

    @Override
    public void stop(){
        spinnerPow = 0;
        setSpinner(0);
    }

    @Override
    public void update(){
        setSpinner(spinnerPow);
    }


}
