package org.firstinspires.ftc.teamcode.subsystems.outtake;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.util.Utility;

public class Shooter implements Subsystem {

    //---------------- Hardware ----------------
    private DcMotorEx leftFlywheel;
    private DcMotorEx rightFlywheel;
    private Servo hood;

    //---------------- Software ----------------
    private Utility util;
    private double flywheelPow = 0.0;

    //---------------- Constructor ----------------
    public Shooter(HardwareMap map) {
//        leftFlywheel = map.get(DcMotorEx.class, "fly_left");
//        rightFlywheel = map.get(DcMotorEx.class, "fly_right");
//        leftFlywheel.setDirection(DcMotorSimple.Direction.REVERSE);
//        rightFlywheel.setDirection(DcMotorSimple.Direction.FORWARD);
//        hood = map.get(Servo.class, "hood");

    }

    //---------------- Core Methods ----------------
    private void setLeftFlywheelPow(double pow){
        leftFlywheel.setPower(pow);
    }

    private void setRightFlywheelPow(double pow){
        rightFlywheel.setPower(pow);
    }

    private void setFlywheelPow(double pow){
        leftFlywheel.setPower(pow);
        rightFlywheel.setPower(pow);
    }

    private void setHood(double target){
        hood.setPosition(target);
    }

    //---------------- Methods ----------------



    //---------------- Interface Methods ----------------
    @Override
    public void toInit(){

    }

    @Override
    public void logPsiKitData() {

    }

    @Override
    public void update(){
//        setFlywheelPow(flywheelPow);
    }

    @Override
    public void stop(){}
}
