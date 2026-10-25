package org.firstinspires.ftc.teamcode.subsystems.intake;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;

public class Spinner implements Subsystem {

    //---------------- Hardware ----------------

    private DigitalChannel sensor1; // The first sensor broken when pollen is intaked
    private DigitalChannel sensor2; // The second sensor broken when pollen is intaked
    private DigitalChannel sensor3; // The third sensor broken when pollen is intaked
    private DigitalChannel sensor4; // The last sensor broken when pollen is intaked and right before outtake
    private DcMotorEx spinner;

    //---------------- Software ----------------
    private double spinnerPow = 0.0;


    //---------------- Constructor ----------------
    public Spinner(HardwareMap map) {
        sensor1 = map.get(DigitalChannel.class, "distance_1");
        sensor2 = map.get(DigitalChannel.class, "distance_2");
        sensor3 = map.get(DigitalChannel.class, "distance_3");
        sensor4 = map.get(DigitalChannel.class, "distance_4");

        spinner = map.get(DcMotorEx.class, "spinner");
        spinner.setDirection(DcMotorSimple.Direction.REVERSE);
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
    public boolean isSensor1Broken() { return !sensor1.getState(); }
    public boolean isSensor2Broken() { return !sensor2.getState(); }
    public boolean isSensor3Broken() { return !sensor3.getState(); }
    public boolean isSensor4Broken() { return !sensor4.getState(); }
    
    public int getBallCount() {
        int count = 0;
        if (isSensor1Broken()) count++;
        if (isSensor2Broken()) count++;
        if (isSensor3Broken()) count++;
        if (isSensor4Broken()) count++;
        return count;
    }
    //---------------- Interface Methods ----------------
    @Override
    public void init(){}

    @Override
    public void update(){
        setSpinner(spinnerPow);
    }


}
