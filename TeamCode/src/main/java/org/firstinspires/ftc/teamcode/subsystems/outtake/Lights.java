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

    //---------------- Colors (servo positions, Panels) ----------------
    // goBILDA's chart runs in rainbow order: 0.277 red, 0.333 orange, 0.388 yellow, 0.444 sage, 0.500 green,
    // 0.555 azure, 0.611 blue, 0.722 violet, 1.0 white. Orange and true green are far apart, so they're easy to
    // tell apart (yellow 0.388 and last year's green 0.475 looked alike). Tune in Panels if a color is off
    public static double OFF = 0.0;
    public static double ORANGE = 0.333;
    public static double GREEN = 0.500;

    //---------------- Hardware ----------------
    private Servo lightLeft;
    private Servo lightRight;

    //---------------- Software ----------------
    // NaN = never written, so the first color always goes out
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
    // Only marks a change when the color is different, so the same color isn't rewritten every loop
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
