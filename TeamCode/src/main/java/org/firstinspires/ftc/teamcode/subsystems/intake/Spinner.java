package org.firstinspires.ftc.teamcode.subsystems.intake;

import com.qualcomm.robotcore.hardware.DcMotor;
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
    private int ballCount;
    private boolean lastSensor1Broken = false;
    private boolean lastSensor4Broken = false;
    private double spinnerPow = 0.0;


    //---------------- Constructor ----------------
    public Spinner(HardwareMap map) {
        sensor1 = map.get(DigitalChannel.class, "distance_1");
        sensor2 = map.get(DigitalChannel.class, "distance_2");
        sensor3 = map.get(DigitalChannel.class, "distance_3");
        sensor4 = map.get(DigitalChannel.class, "distance_4");

        sensor1.setMode(DigitalChannel.Mode.INPUT);
        sensor2.setMode(DigitalChannel.Mode.INPUT);
        sensor3.setMode(DigitalChannel.Mode.INPUT);
        sensor4.setMode(DigitalChannel.Mode.INPUT);

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
    public boolean isSensor1Broken() { return !sensor1.getState(); }
    public boolean isSensor2Broken() { return !sensor2.getState(); }
    public boolean isSensor3Broken() { return !sensor3.getState(); }
    public boolean isSensor4Broken() { return !sensor4.getState(); }
    public int getBallCount() {
        return ballCount;
    }

    public void setBallCount(int count) { //Use for preload in auto
        this.ballCount = Math.max(0, Math.min(4, count)); // Keep within bounds [0, 4]
    }

    public void initBallCount() {
        ballCount = 0;
        lastSensor1Broken = isSensor1Broken();
        lastSensor4Broken = isSensor4Broken();
    }

    public void updateBallCount() {
        boolean current1Broken = isSensor1Broken();
        boolean current4Broken = isSensor4Broken();

        // 1. ENTRY (Rising Edge on Sensor 1 while Intaking): Ball breaks entry beam
        if (current1Broken && !lastSensor1Broken && spinnerPow > 0) {
            ballCount = Math.min(4, ballCount + 1);
        }

        // 2. EXIT (Falling Edge on Sensor 4 while Intaking): Ball leaves exit beam into outtake
        if (!current4Broken && lastSensor4Broken && spinnerPow > 0) {
            ballCount = Math.max(0, ballCount - 1);
        }

        // 3. EJECT (Falling Edge on Sensor 1 while Reversing): Ball spit out front
        if (!current1Broken && lastSensor1Broken && spinnerPow < 0) {
            ballCount = Math.max(0, ballCount - 1);
        }

        // Update state memory for next loop cycle
        lastSensor1Broken = current1Broken;
        lastSensor4Broken = current4Broken;
    }
    //---------------- Interface Methods ----------------
    @Override
    public void init(){
        stop();
        initBallCount();
    }

    @Override
    public void stop(){
        spinnerPow = 0;
        setSpinner(0);
    }

    @Override
    public void update(){
        setSpinner(spinnerPow);
        updateBallCount();
    }


}
