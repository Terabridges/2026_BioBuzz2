package org.firstinspires.ftc.teamcode.subsystems.outtake;

import com.arcrobotics.ftclib.controller.PIDFController;
import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.utility.Utility;

@Configurable
public class Shooter implements Subsystem {

    //---------------- Hardware ----------------
    private final DcMotorEx flywheel;
    private final Servo hood;
    private final HardwareMap.DeviceMapping<VoltageSensor> voltageSensors;

    //---------------- Software ----------------
    private final Utility util = new Utility();
    private final double hoodStart = 0.76;

    private double flywheelPow = 0.0;
    private double hoodPos = hoodStart;
    private boolean hoodChanged = false;
    private final double hoodMin = 0.39;
    private final double hoodMax = 0.82;
    public static double hoodStep = 0.02;

    public static double shootRPM = 2000;
    public static double rpmStep = 100;
    private final double maxRPM = 6000;
    private static final double NOMINAL_VOLTAGE = 12.0;

    public static double TICKS_PER_REV = 4000.0; // ELC Encoder V2 (4000 CPR)
    public static double SHOOTER_GEAR_RATIO = 1.0;
    public static double encoderSign = 1.0; // Flips decreasing ticks to positive outward RPM
    public static double minSampleMs = 25.0;
    public static boolean enableRpmSmoothing = true;
    public static double rpmSmoothing = 0.15; // Weight of the newest RPM sample.
    private int lastPos = 0;
    private long lastTimeNs = 0;
    private boolean hasLastReading = false;

    private final PIDFController flywheelPID;
    public static double p = 0.0045, i = 0.0, d = 0.00004, f = 0.000227;
    public static boolean useLegacyPidGains = false;
    private static final double LEGACY_P = 0.0014, LEGACY_I = 0.00008, LEGACY_D = 0.0, LEGACY_F = 0.000213;
    private boolean usingLegacyPidGains;
    public static double integrationBounds = 200;
    public static double posTolerance = 100;
    public static double shotSettleTimeMs = 150.0;
    private final ElapsedTime shotSettleTimer = new ElapsedTime();
    private boolean insideShotSpeedWindow;
    private boolean useFlywheelPID = true;

    public static double flywheelTargetRPM = 0;
    private double currentRPM = 0;
    private double rawRPM;
    private double flywheelMaxPower = 1.0;

    private double flywheelOffset = 0;
    private double hoodOffset = 0;
    private double newRPM = flywheelTargetRPM + flywheelOffset;

    // Position-based RPM tracking

    // Voltage caching variables
    private final ElapsedTime voltageTimer = new ElapsedTime();
    private double cachedBatteryVoltage = NOMINAL_VOLTAGE;
    public static boolean useCachedVoltageCompensation = true;
    public static double voltageRefreshRateMs = 250.0;

    // Diagnostic heartbeat counter
    private int updateCount = 0;


    //---------------- Constructor ----------------
    public Shooter(HardwareMap map) {
        voltageSensors = map.voltageSensor;
        flywheel = map.get(DcMotorEx.class, "flywheel");
        flywheel.setDirection(DcMotorSimple.Direction.REVERSE);
        flywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        flywheel.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        hood = map.get(Servo.class, "hood");

        flywheelPID = new PIDFController(p, i, d, f);
        stop();
        flywheelPID.setIntegrationBounds(-integrationBounds, integrationBounds);
        flywheelPID.setTolerance(posTolerance);
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
        return RPMToVel(currentRPM);
    }

    public double velToRPM(double tps) {
        double rps = tps / (TICKS_PER_REV * SHOOTER_GEAR_RATIO);
        return rps * 60;
    }

    public double RPMToVel(double rpm) {
        double tpm = rpm * TICKS_PER_REV * SHOOTER_GEAR_RATIO;
        return tpm / 60;
    }

    private double getPositionRPM() {
        int currentPos = flywheel.getCurrentPosition();
        long currentTimeNs = System.nanoTime();

        if (!hasLastReading) {
            lastPos = currentPos;
            lastTimeNs = currentTimeNs;
            hasLastReading = true;
            return currentRPM;
        }

        long elapsedNs = currentTimeNs - lastTimeNs;
        if (elapsedNs >= (long) (minSampleMs * 1_000_000.0)) {
            double deltaTicks = (currentPos - lastPos) * encoderSign;
            double deltaSeconds = elapsedNs / 1_000_000_000.0;
            rawRPM = velToRPM(deltaTicks / deltaSeconds);

            currentRPM = enableRpmSmoothing
                    ? (rpmSmoothing * rawRPM) + ((1.0 - rpmSmoothing) * currentRPM)
                    : rawRPM;

            lastPos = currentPos;
            lastTimeNs = currentTimeNs;
        }

        return currentRPM;
    }

    public double setFlywheelPID(double targetRPM) {
        if (usingLegacyPidGains != useLegacyPidGains) {
            flywheelPID.reset();
            usingLegacyPidGains = useLegacyPidGains;
        }
        flywheelPID.setPIDF(getActiveP(), usingLegacyPidGains ? LEGACY_I : i,
                usingLegacyPidGains ? LEGACY_D : d, getActiveF());
        currentRPM = getPositionRPM();
        double requestedPower = flywheelPID.calculate(currentRPM, targetRPM);
        flywheelPow = util.clamp(requestedPower * batteryCompensation(), 0, flywheelMaxPower);
        return flywheelPow;
    }

    private double batteryCompensation() {
        if (!useCachedVoltageCompensation) {
            double batteryVoltage = readBatteryVoltage();
            return Double.isFinite(batteryVoltage) ? NOMINAL_VOLTAGE / batteryVoltage : 1.0;
        }
        if (voltageTimer.milliseconds() > voltageRefreshRateMs) {
            double batteryVoltage = readBatteryVoltage();
            if (Double.isFinite(batteryVoltage)) {
                cachedBatteryVoltage = batteryVoltage;
            }
            voltageTimer.reset();
        }
        return NOMINAL_VOLTAGE / cachedBatteryVoltage;
    }

    private double readBatteryVoltage() {
        double batteryVoltage = Double.POSITIVE_INFINITY;
        for (VoltageSensor sensor : voltageSensors) {
            double voltage = sensor.getVoltage();
            if (voltage > 0 && Double.isFinite(voltage)) batteryVoltage = Math.min(batteryVoltage, voltage);
        }
        return batteryVoltage;
    }

    public void setFlywheelRPM(double target){
        setFlywheelPow(setFlywheelPID(target));
    }

    public double getCurrentRPM(){
        return currentRPM;
    }

    public double getRawRPM() { return rawRPM; }

    // True when the flywheel is on and within tolerance (RPM) of its target
    public boolean isAtRPM(double tolerance){
        return isShooting() && Math.abs(getTargetRPM() - currentRPM) <= tolerance;
    }

    public void updateShotSpeedReadiness(double tolerance) {
        boolean insideWindow = isAtRPM(tolerance);
        if (insideWindow && !insideShotSpeedWindow) shotSettleTimer.reset();
        insideShotSpeedWindow = insideWindow;
    }

    public boolean isShotSpeedSettled() {
        return insideShotSpeedWindow && shotSettleTimer.milliseconds() >= shotSettleTimeMs;
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

    public double getActiveP() { return usingLegacyPidGains ? LEGACY_P : p; }
    public double getActiveF() { return usingLegacyPidGains ? LEGACY_F : f; }

    public int getUpdateCount() {
        return updateCount;
    }

    public double getCurrentPosition() {
        return flywheel.getCurrentPosition() * encoderSign;
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
        setHoodPos(hoodStart);
    }

    @Override
    public void stop(){
        flywheelTargetRPM = 0;
        flywheelOffset = 0;
        newRPM = 0;
        flywheelPow = 0;
        hasLastReading = false;
        rawRPM = 0;
        hoodChanged = false;
        insideShotSpeedWindow = false;
        flywheelPID.reset();
        flywheel.setPower(0);
    }

    @Override
    public void logPsiKitData() {

    }

    @Override
    public void update(){
        updateCount++;
        newRPM = flywheelTargetRPM + flywheelOffset;

        if (useFlywheelPID) {
            if (newRPM <= 0) {
                flywheelPID.reset();
                flywheelPow = 0;
                currentRPM = getPositionRPM();
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
