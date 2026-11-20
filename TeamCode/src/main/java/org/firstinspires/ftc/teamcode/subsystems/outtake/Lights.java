package org.firstinspires.ftc.teamcode.subsystems.outtake;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;

public class Lights implements Subsystem {

    //---------------- Hardware ----------------
    private Servo lightLeft;
    private Servo lightRight;

    //---------------- Software ----------------
    private double lightLeftPos = 0;
    private boolean lightLeftChanged = false;
    private double lightRightPos = 0;
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
    //TODO: Find light intervals for:
    /*
        Yellow: Check 0.40-0.50 (PWM 1300 - 1500 usec), potentially 0.388
        Green: Check 0.40-0.50 (PWM 1300 - 1500 usec), potentially 0.475
        Blue: Check 0.55-0.65 (PWM 1600 - 1800 usec), potentially 0.611
        Red: Check 0.25-0.35 (PWM 1000 - 1200 usec), potentially 0.280
     */
    public void setLightLeftPos(double pos){
        lightLeftChanged = true;
        lightLeftPos = pos;
    }

    public void setLightRightPos(double pos){
        lightRightChanged = true;
        lightRightPos = pos;
    }

    public void setLights(double pos){
        setLightLeftPos(pos);
        setLightRightPos(pos);
    }

    public void setLightsAuto(Intake intake, Outtake outtake) {
        int ballCount = intake.spinner.getBallCount();
        if (outtake.shooter.hasFaultOrTimeout(30)) {
            setLights(0.280); //Check!
        } else {
            if (ballCount == 0) {
                setLights(0);
            } else if (ballCount == 1 || ballCount == 2 || ballCount == 3) {
                setLights(0.388); //Check!
            } else if (ballCount == 4 && outtake.shooter.isAtRPM(30)) {
                setLights(0.611); //Check!
            } else if (ballCount == 4) {
                setLights(0.475); //Check!
            }
        }
    }

    //---------------- Interface Methods ----------------
    @Override
    public void init(){}

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
