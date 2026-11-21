package org.firstinspires.ftc.teamcode.subsystems.outtake;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;

/**
 * Two goBILDA RGB indicator lights. They're plugged into servo ports, and the servo position picks the color.
 */
@Configurable
public class Lights implements Subsystem {

    //---------------- Colors ----------------
    public static double OFF = 0.0;
    public static double ORANGE = 0.333;
    public static double GREEN = 0.500;

    //---------------- Hardware ----------------
    private Servo lightLeft;
    private Servo lightRight;

    //---------------- Software ----------------
    private double lightLeftPos = Double.NaN;
    private boolean lightLeftChanged = false;
    private double lightRightPos = Double.NaN;
    private boolean lightRightChanged = false;

    //---------------- Constructor ----------------
    public Lights(HardwareMap map) {
        lightLeft = map.get(Servo.class, "light_left");
        lightRight = map.get(Servo.class, "light_right");
    }

    //---------------- Core Methods ----------------
    private void setLightLeft(double pos){
        lightLeft.setPosition(pos);
    }

    private void setLightRight(double pos){
        lightRight.setPosition(pos);
    }

    //---------------- Methods ----------------
    public void setLightLeftPos(double pos){
        if (pos == lightLeftPos) return;
        lightLeftChanged = true;
        lightLeftPos = pos;
    }

    public void setLightRightPos(double pos){
        if (pos == lightRightPos) return;
        lightRightChanged = true;
        lightRightPos = pos;
    }

    public void setLights(double pos){
        setLightLeftPos(pos);
        setLightRightPos(pos);
    }

    //---------------- Interface Methods ----------------
    @Override
    public void init(){
        setLights(OFF);
    }

    @Override
    public void update(){
        if (lightLeftChanged){
            setLightLeft(lightLeftPos);
            lightLeftChanged = false;
        }
        if (lightRightChanged){
            setLightRight(lightRightPos);
            lightRightChanged = false;
        }
    }
}
