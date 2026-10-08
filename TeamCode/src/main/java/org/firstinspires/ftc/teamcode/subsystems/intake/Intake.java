package org.firstinspires.ftc.teamcode.subsystems.intake;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;

public class Intake implements Subsystem {
    //---------------- Hardware -----------------
    DcMotor intake;

    //---------------- Subsystems ----------------


    //---------------- Software ----------------
    private double megaSpinPow = 0;
    private double overridePow = 0;
    public boolean spinOverride = false;
    public boolean autoSpin = true;
    private boolean useMegaSpin = true;


    //---------------- Constructor ----------------
    public Intake(HardwareMap map) {
        intake = map.get(DcMotor.class, "Intake");
    }

    //---------------- Methods ----------------
    private void moveMegaSpinPow(double pow){
        intake.setPower(pow);
    }

    public void setMegaSpinPow(double pow){
        megaSpinPow = pow;
    }

    public void setMegaSpinIn(){
        megaSpinPow = 0.98;
        overrideSpinZero();
    }

    public void setMegaSpinOut(){
        megaSpinPow = -0.98;
        overrideSpinZero();
    }

    public void setMegaSpinZero(){
        megaSpinPow = 0;
        overrideSpinZero();
    }

    public void overrideSpinIn(){
        overridePow = 0.8;
        spinOverride = true;
    }

    public void overrideSpinOut(){
        overridePow = -0.8;
        spinOverride = true;
    }

    public void overrideSpinZero(){
        overridePow = 0;
        spinOverride = false;
    }

    //---------------- Interface Methods ----------------
    @Override
    public void toInit(){}

    @Override
    public void update(){
        if(spinOverride && autoSpin){
            moveMegaSpinPow(overridePow);
        } else if (useMegaSpin){
            moveMegaSpinPow(megaSpinPow);
        }
    }

    @Override
    public void stop(){}
}
