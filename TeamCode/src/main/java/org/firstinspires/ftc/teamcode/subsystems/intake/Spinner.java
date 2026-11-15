package org.firstinspires.ftc.teamcode.subsystems.intake;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.utility.intakeUtility.RealTimeBallFilter;

public class Spinner implements Subsystem {

    //---------------- Hardware ----------------
    private DigitalChannel sensor1; // Entry sensor
    private DigitalChannel sensor2; // Mid-path 1
    private DigitalChannel sensor3; // Mid-path 2
    private DigitalChannel sensor4; // Exit/Outtake sensor
    private DcMotorEx spinner;

    //---------------- Software ----------------
    private int ballCount = 0;
    private double spinnerPow = 0.0;

    // Timing & Filtering
    private final ElapsedTime sensor1Timer = new ElapsedTime();
    private final ElapsedTime sensor4Timer = new ElapsedTime();
    private final ElapsedTime sensor1HoldTimer = new ElapsedTime();

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
        sensor1Timer.reset();
        sensor4Timer.reset();
        sensor1HoldTimer.reset();
    }

    private final RealTimeBallFilter ballFilter = new RealTimeBallFilter();
    public void updateBallCount() {
        boolean[] sensors = {
                isSensor1Broken(),
                isSensor2Broken(),
                isSensor3Broken(),
                isSensor4Broken()
        };

        int rawCount = 0;
        int clusterSize = 0;

        for (boolean b : sensors) {
            if (b) {
                clusterSize++;
            } else {
                if (clusterSize > 0) {
                    if (spinnerPow == 0) {
                        rawCount += clusterSize;
                    } else {
                        // Apply overlap protection strictly to 2-sensor clusters (1 ball straddling)
                        if (clusterSize == 2) {
                            rawCount += 1;
                        } else {
                            // 1 sensor = 1 ball. 3 sensors = 3 balls.
                            rawCount += clusterSize;
                        }
                    }
                    clusterSize = 0;
                }
            }
        }

        // Evaluate the final cluster at the end of the array
        if (clusterSize > 0) {
            if (spinnerPow == 0) {
                rawCount += clusterSize;
            } else {
                if (clusterSize == 2) {
                    rawCount += 1;
                } else {
                    rawCount += clusterSize;
                }
            }
        }

        ballCount = ballFilter.update(rawCount);
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