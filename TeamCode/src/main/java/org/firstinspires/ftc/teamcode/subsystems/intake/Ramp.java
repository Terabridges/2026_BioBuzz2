package org.firstinspires.ftc.teamcode.subsystems.intake;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.utility.Utility;

public class Ramp implements Subsystem {

    //---------------- Hardware ----------------
    private Servo rampLeft;
    private Servo rampRight;
    private Utility util;

    //---------------- Software ----------------
    private final double rightRampOffset = 0.024;
    private final double rampStart = 0.5;
    private double rampPos = rampStart;

    private boolean rampChanged = false;
    private boolean rampUp = false;

    private final double rampMinPos = 0.475;
    private final double rampMaxPos = 0.7;
    private final double rampDownPos = 0.475;
    private final double rampUpPos = 0.695;

    //---------------- Constructor ----------------
    public Ramp(HardwareMap map) {
        rampLeft = map.get(Servo.class, "ramp_left");
        rampRight = map.get(Servo.class, "ramp_right");
        rampLeft.setDirection(Servo.Direction.REVERSE);
        util = new Utility();
    }

    //---------------- Core Methods ----------------
    // The min/max limit the left servo's position (the one the code asks for, measured with Ramp Test). The right
    // servo follows at left + rightRampOffset, so it can go up to rampMaxPos + rightRampOffset
    public void setRamp(double pos){
        pos = util.clamp(pos, rampMinPos, rampMaxPos);
        rampLeft.setPosition(pos);
        rampRight.setPosition(pos+rightRampOffset);
    }


    //---------------- Methods ----------------
    public void setRampPos(double pos){
        rampChanged = true;
        rampPos = pos;
    }

    public void setRampUp(){
        rampUp = true;
        setRampPos(rampUpPos);
    }

    public void setRampDown(){
        rampUp = false;
        setRampPos(rampDownPos);
    }

    public void toggleRamp(){
        if (rampUp) setRampDown();
        else setRampUp();
    }

    public boolean isRampUp(){return rampUp;}

    public double getRightRampOffset(){return rightRampOffset;}

    //---------------- Interface Methods ----------------
    @Override
    public void stop(){
        rampChanged = false;
    }

    @Override
    public void init(){
        setRampUp();
    }

    @Override
    public void update(){
        if (rampChanged){
            setRamp(rampPos);
            rampChanged = false;
        }
    }
}
