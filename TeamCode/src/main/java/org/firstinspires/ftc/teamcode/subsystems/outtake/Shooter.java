package org.firstinspires.ftc.teamcode.subsystems.outtake;

import com.arcrobotics.ftclib.controller.PIDFController;
import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.utility.Utility;

@Configurable
public class Shooter implements Subsystem {

    //---------------- Hardware ----------------
    private DcMotorEx flywheel;
    private Servo hood;

    //---------------- Software ----------------
    private final Utility util = new Utility();
    private final double hoodStart = 0.5;

    private double flywheelPow = 0.0;
    private double hoodPos = hoodStart;
    private boolean hoodChanged = false;
    private final double hoodMin = 0.39;
    private final double hoodMax = 0.82;
    public static double hoodStep = 0.02;

    public static double shootRPM = 2000;
    public static double rpmStep = 100;
    private final double maxRPM = 6000;

    private final double TICKS_PER_REV = 28.0; // goBILDA 5202/5203
    private final double SHOOTER_GEAR_RATIO = 1.0;
    private PIDFController flywheelPID;

    // Kept your specified P and D gains
    public static double p = 0.00005, i = 0.0000, d = 0.0002, f = 0.000213;

    public static double flywheelTargetRPM = 0;
    private double currentRPM = 0;
    private double flywheelMaxPower = 1.0;
    private boolean useFlywheelPID = true;

    private double flywheelOffset = 0;
    private double hoodOffset = 0;

    private double newRPM = flywheelTargetRPM + flywheelOffset;

    public static boolean enableFiltering = true;

    // HEAVY FILTER: 0.05 only trusts 5% of the new reading, smoothing the jagged encoder
    // steps into a curve so your high D-gain doesn't freak out.
    public static double emaAlpha = 0.1;

    private double emaFilteredRPM = 0.0;
    private boolean filterInitialized = false;
    private double currentRampedRPM = 0.0;
    private long lastTime = System.nanoTime();

    public static double rpmRampRate = 2500.0;

    //---------------- Constructor ----------------
    public Shooter(HardwareMap map) {
        flywheel = map.get(DcMotorEx.class, "flywheel");
        flywheel.setDirection(DcMotorSimple.Direction.FORWARD);
        flywheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        flywheel.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        hood = map.get(Servo.class, "hood");

        flywheelPID = new PIDFController(p, i, d, f);
        stop();
    }

    //---------------- Core Methods ----------------
    private void setFlywheel(double pow){
        flywheel.setPower(pow);
    }

    private void setHood(double pos){
        hood.setPosition(util.clamp(pos, hoodMin, hoodMax));
    }

    //---------------- Methods ----------------
    public void setFlywheelPow(double pow){
        flywheelPow = pow;
    }

    public void setHoodPos(double pos){
        hoodChanged = true;
        hoodPos = util.clamp(pos, hoodMin, hoodMax);
    }

    public void hoodUp(){
        setHoodPos(hoodPos + hoodStep);
    }

    public void hoodDown(){
        setHoodPos(hoodPos - hoodStep);
    }

    public double getHoodPos(){return hoodPos;}

    public boolean isShooting(){
        return flywheelTargetRPM > 0;
    }

    public void toggleShooting(){
        setTargetRPM(isShooting() ? 0 : shootRPM);
    }

    public void adjustShootRPM(double delta){
        shootRPM = util.clamp(shootRPM + delta, 0, maxRPM);
        if (isShooting()) {
            setTargetRPM(shootRPM);
        }
    }

    public void increaseShootRPM(){
        adjustShootRPM(rpmStep);
    }

    public void decreaseShootRPM(){
        adjustShootRPM(-rpmStep);
    }

    public double getShootRPM(){return shootRPM;}

    public double getFlywheelVel() {
        return flywheel.getVelocity();
    }

    public double velToRPM(double tps) {
        double rps = tps / (TICKS_PER_REV * SHOOTER_GEAR_RATIO);
        return rps * 60;
    }

    public double RPMToVel(double rpm) {
        double tpm = rpm * TICKS_PER_REV * SHOOTER_GEAR_RATIO;
        return tpm / 60;
    }

    public double setFlywheelPID(double targetRPM) {
        double rawRPM = velToRPM(flywheel.getVelocity());

        if (enableFiltering) {
            if (!filterInitialized) {
                emaFilteredRPM = rawRPM;
                filterInitialized = true;
            } else {
                emaFilteredRPM = (emaAlpha * rawRPM) + ((1.0 - emaAlpha) * emaFilteredRPM);
            }
            currentRPM = emaFilteredRPM;
        } else {
            currentRPM = rawRPM;
        }

        double trueError = Math.abs(newRPM - currentRPM);

        // 1. Dynamic P-Gain for smooth deceleration into the target
        double dynamicP = p; // Defaults to your ultra-low 0.00005 for steady-state

        if (trueError > 200) {
            // At 1500 error, this commands max power.
            // At 500 error, it smoothly curves down to ~70% power.
            // At 100 error, it transitions seamlessly to your steady-state gains.
            dynamicP = 0.001;
        }

        // 2. Standard PID Calculation
        flywheelPID.setPIDF(dynamicP, i, d, f);
        flywheelPow = flywheelPID.calculate(currentRPM, targetRPM);

        return util.clamp(flywheelPow, 0, flywheelMaxPower);
    }

    public void setFlywheelRPM(double target){
        setFlywheelPow(setFlywheelPID(target));
    }

    public double getCurrentRPM(){
        return currentRPM;
    }

    public void setTargetRPM(double rpm){
        flywheelTargetRPM = rpm;
    }

    public double getTargetRPM(){
        return flywheelTargetRPM + flywheelOffset;
    }

    public double getCurrentPower(){
        return flywheelPow;
    }

    public boolean getUseFlywheel(){return useFlywheelPID;}

    public void toggleUseFlywheel(){
        useFlywheelPID = !useFlywheelPID;
        flywheelPID.reset();
        if (!useFlywheelPID) {
            flywheelPow = 0;
        }
    }

    //---------------- Interface Methods ----------------
    @Override
    public void init(){
        stop();
    }

    @Override
    public void stop(){
        flywheelTargetRPM = 0;
        flywheelOffset = 0;
        newRPM = 0;
        currentRampedRPM = 0;
        lastTime = System.nanoTime();
        flywheelPow = 0;
        hoodChanged = false;
        filterInitialized = false;
        flywheelPID.reset();
        flywheel.setPower(0);
    }

    @Override
    public void logPsiKitData() {

    }

    @Override
    public void update(){
        long now = System.nanoTime();
        double dt = (now - lastTime) / 1e9;
        lastTime = now;

        newRPM = flywheelTargetRPM + flywheelOffset;

        if (useFlywheelPID) {
            if (newRPM <= 0) {
                flywheelPID.reset();
                flywheelPow = 0;
                currentRampedRPM = 0;
            } else {
                if (currentRampedRPM < newRPM) {
                    currentRampedRPM += (rpmRampRate * dt);
                    if (currentRampedRPM > newRPM) currentRampedRPM = newRPM;
                } else if (currentRampedRPM > newRPM) {
                    currentRampedRPM -= (rpmRampRate * dt);
                    if (currentRampedRPM < newRPM) currentRampedRPM = newRPM;
                }

                setFlywheelRPM(currentRampedRPM);
            }
        }

        setFlywheel(flywheelPow);

        if (hoodChanged){
            setHood(hoodPos);
            hoodChanged = false;
        }
    }
}