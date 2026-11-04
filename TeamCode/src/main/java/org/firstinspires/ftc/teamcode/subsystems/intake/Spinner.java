package org.firstinspires.ftc.teamcode.subsystems.intake;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;

public class Spinner implements Subsystem {

    //---------------- Hardware ----------------
    private DigitalChannel sensor1; // Entry sensor
    private DigitalChannel sensor2; // Mid-path 1
    private DigitalChannel sensor3; // Mid-path 2
    private DigitalChannel sensor4; // Exit/Outtake sensor
    private DcMotorEx spinner;

    //---------------- Software ----------------
    private int ballCount = 0;
    private boolean lastSensor1Broken = false;
    private boolean lastSensor4Broken = false;
    private double spinnerPow = 0.0;

    // Timing & Filtering
    private final ElapsedTime sensor1Timer = new ElapsedTime();
    private final ElapsedTime sensor4Timer = new ElapsedTime();
    private final ElapsedTime sensor1HoldTimer = new ElapsedTime();

    private static final double COOLDOWN_SEC = 0.15; // Ignore flickers for 150ms after an edge
    private static final double MULTI_BALL_HOLD_SEC = 0.35; // If broken > 350ms while intaking, count 2nd ball

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
        spinner.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    //---------------- Core Methods ----------------
    private void setSpinner(double pow) {
        spinner.setPower(pow);
    }

    //---------------- Methods ----------------
    public void setSpinnerPow(double pow) { spinnerPow = pow; }
    public void setSpinnerSpinIn() { spinnerPow = 0.98; }
    public void setSpinnerSpinOut() { spinnerPow = -0.98; }
    public void setSpinnerSpinIn(double pow) { spinnerPow = Math.abs(pow); }
    public void setSpinnerSpinOut(double pow) { spinnerPow = -Math.abs(pow); }
    public void setSpinnerSpinZero() { spinnerPow = 0.0; }

    public boolean isSensor1Broken() { return sensor1.getState(); }
    public boolean isSensor2Broken() { return sensor2.getState(); }
    public boolean isSensor3Broken() { return sensor3.getState(); }
    public boolean isSensor4Broken() { return sensor4.getState(); }

    public int getBallCount() { return ballCount; }
    public void setBallCount(int count) { this.ballCount = Math.max(0, Math.min(4, count)); }
    public boolean isFull() { return ballCount >= 4; }

    public void initBallCount() {
        lastSensor1Broken = isSensor1Broken();
        lastSensor4Broken = isSensor4Broken();
        sensor1Timer.reset();
        sensor4Timer.reset();
        sensor1HoldTimer.reset();
    }

    public void updateBallCount() {
        boolean current1Broken = isSensor1Broken();
        boolean current2Broken = isSensor2Broken();
        boolean current3Broken = isSensor3Broken();
        boolean current4Broken = isSensor4Broken();

        // --- HARDWARE SANITY CHECKS ---
        // 1. ALL CLEAR: Force count to 0 if no sensors are detecting a ball
        if (!current1Broken && !current2Broken && !current3Broken && !current4Broken) {
            ballCount = 0;
            lastSensor1Broken = false;
            lastSensor4Broken = false;
            return;
        }

        // 2. ALL FULL: Force count to 4 if all sensors are blocked
        if (current1Broken && current2Broken && current3Broken && current4Broken) {
            ballCount = 4;
        }

        // --- INTAKE ENTRY (Sensor 1) ---
        if (current1Broken && !lastSensor1Broken) {
            sensor1HoldTimer.reset(); // Reset hold timer when beam first breaks
            if (spinnerPow > 0 && sensor1Timer.seconds() > COOLDOWN_SEC) {
                ballCount = Math.min(4, ballCount + 1);
                sensor1Timer.reset();
            }
        }

        // Catch continuous touching balls (Sensor 1 stays broken while intaking)
        if (current1Broken && spinnerPow > 0 && sensor1HoldTimer.seconds() > MULTI_BALL_HOLD_SEC) {
            if (sensor1Timer.seconds() > COOLDOWN_SEC) {
                ballCount = Math.min(4, ballCount + 1);
                sensor1Timer.reset();
                sensor1HoldTimer.reset(); // Reset to catch a 3rd touching ball if needed
            }
        }

        // --- OUTTAKE EXIT (Sensor 4) ---
        if (!current4Broken && lastSensor4Broken && spinnerPow > 0) {
            if (sensor4Timer.seconds() > COOLDOWN_SEC) {
                ballCount = Math.max(0, ballCount - 1);
                sensor4Timer.reset();
            }
        }

        // --- EJECT FRONT (Sensor 1) ---
        if (!current1Broken && lastSensor1Broken && spinnerPow <= 0) {
            if (sensor1Timer.seconds() > COOLDOWN_SEC) {
                ballCount = Math.max(0, ballCount - 1);
                sensor1Timer.reset();
            }
        }

        // --- RE-ENTRY FROM OUTTAKE (Sensor 4) ---
        if (current4Broken && !lastSensor4Broken && spinnerPow < 0) {
            if (sensor4Timer.seconds() > COOLDOWN_SEC) {
                ballCount = Math.min(4, ballCount + 1);
                sensor4Timer.reset();
            }
        }

        lastSensor1Broken = current1Broken;
        lastSensor4Broken = current4Broken;
    }

    //---------------- Interface Methods ----------------
    @Override
    public void init() {
        stop();
        initBallCount();
    }

    @Override
    public void stop() {
        spinnerPow = 0;
        setSpinner(0);
    }

    @Override
    public void update() {
        setSpinner(spinnerPow);
        updateBallCount();
    }
}