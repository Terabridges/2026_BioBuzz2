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
    private DcMotorEx flywheelLeft;
    private DcMotorEx flywheelRight;
    private Servo hood;

    //---------------- Software ----------------
    private final Utility util = new Utility();
    private final double hoodStart = 0.5;
    private double flywheelPow = 0.0;
    private double hoodPos = hoodStart;
    private boolean hoodChanged = false;
    private final double hoodMin = 0.39;
    private final double hoodMax = 0.82;
    // TODO verify that increasing the position raises the hood
    public static double hoodStep = 0.02;

    public static double shootRPM = 2000;
    public static double rpmStep = 100;
    private final double maxRPM = 6000;

    private final double TICKS_PER_REV = 28.0; // goBILDA 5202/5203
    private final double SHOOTER_GEAR_RATIO = 1.0;
    private PIDFController flywheelPID;
    public static double p = 0.0014, i = 0.00008, d = 0.0, f = 0.00023;
    //OLD: p = 0.0015, i = 0.0001, d = 0.0, f = 0.0002;
    //NEW: p = 0.0014, i = 0.00008, d = 0.0, f = 0.00023
    //public static double integrationBounds = 250;
    public static double flywheelTargetRPM = 0;
    private double currentRPM = 0;
    //public static double posTolerance = 150;
    private double flywheelMaxPower = 1.0;
    private boolean useFlywheelPID = true;

    private double flywheelOffset = 0;
    private double hoodOffset = 0;

    private double newRPM = flywheelTargetRPM + flywheelOffset;

    //---------------- Constructor ----------------
    public Shooter(HardwareMap map) {
        flywheelLeft = map.get(DcMotorEx.class, "flywheel_left");
        flywheelRight = map.get(DcMotorEx.class, "flywheel_right");
        flywheelLeft.setDirection(DcMotorSimple.Direction.FORWARD);
        flywheelRight.setDirection(DcMotorSimple.Direction.REVERSE);
        flywheelLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        flywheelRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        flywheelLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        flywheelRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        hood = map.get(Servo.class, "hood");

        flywheelPID = new PIDFController(p, i, d, f);
        // The live target remains configurable for the tuner, but never carries into a new OpMode.
        stop();
        //flywheelPID.setIntegrationBounds(-integrationBounds, integrationBounds);
        //flywheelPID.setTolerance(posTolerance);
    }

    //---------------- Core Methods ----------------
    private void setFlywheel(double pow){
        flywheelLeft.setPower(pow);
        flywheelRight.setPower(pow);
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

    // Changes the RPM used when shooting, and applies it right away if already shooting
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

    public double velToRPM(double tps) {
        double rps = tps / (TICKS_PER_REV * SHOOTER_GEAR_RATIO);
        return rps * 60;
    }

    public double RPMToVel(double rpm) {
        double tpm = rpm * TICKS_PER_REV * SHOOTER_GEAR_RATIO;
        return tpm / 60;
    }


    public double setFlywheelPID(double targetRPM) {
        flywheelPID.setPIDF(p, i, d, f);
        currentRPM = velToRPM(flywheelLeft.getVelocity());
        flywheelPow = flywheelPID.calculate(currentRPM, targetRPM);
        flywheelPow = util.clamp(flywheelPow, 0, flywheelMaxPower);
        return flywheelPow;
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
        //setHoodPos(hoodStart);
    }

    @Override
    public void stop(){
        flywheelTargetRPM = 0;
        flywheelOffset = 0;
        newRPM = 0;
        flywheelPow = 0;
        hoodChanged = false;
        flywheelPID.reset();
        try {
            flywheelLeft.setPower(0);
        } finally {
            flywheelRight.setPower(0);
        }
    }

    @Override
    public void logPsiKitData() {

    }

    @Override
    public void update(){

        newRPM = flywheelTargetRPM + flywheelOffset;
        if (useFlywheelPID) {
            if (newRPM <= 0) {
                // A stopped shooter must not receive residual power from PID integral state.
                flywheelPID.reset();
                flywheelPow = 0;
            } else {
                setFlywheelRPM(newRPM);
            }
        }

        setFlywheel(flywheelPow);

        if (hoodChanged){
            setHood(hoodPos);
            hoodChanged = false;
        }
    }
}
