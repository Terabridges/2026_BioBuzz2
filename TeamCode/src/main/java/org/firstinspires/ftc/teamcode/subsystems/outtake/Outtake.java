package org.firstinspires.ftc.teamcode.subsystems.outtake;

import com.bylazar.configurables.annotations.Configurable;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.function.Supplier;
import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.utility.shooterUtility.ShooterData;
import org.firstinspires.ftc.teamcode.utility.shooterUtility.TurretAim;

@Configurable
public class Outtake implements Subsystem {

    //---------------- Subsystems ----------------
    public Shooter shooter;
    public Turret turret;
    public Lights lights;


    //---------------- Software ----------------
    public static double rpmTolerance = 50;
    public static double turretToleranceDeg = 3;
    public static double autoFeedPulseMs = 80;
    public static double autoFeedPauseMs = 40;
    private static final double HOOD_DEADBAND = 0.002;

    private final ShooterData shooterData = new ShooterData();
    private final TurretAim aim = new TurretAim(Alliance.RED);
    private Supplier<Pose> pose;
    private Supplier<Velocity> velocity;
    private TurretAim.Cell targetCell = TurretAim.Cell.CLOSE;
    private boolean trackingEnabled;
    private boolean flywheelEnabled;
    private boolean automaticRequested;
    private boolean pulsedFeedRequested;
    private boolean clearFeedRequested;
    private boolean feedPaused;
    private final ElapsedTime feedPhaseTimer = new ElapsedTime();
    private boolean manualRequested;
    private boolean automaticFiring;
    private boolean manualFiring;
    private double rpmOffset;
    private double hoodOffset;
    private double turretOffsetDeg;
    private double lastHood = Double.NaN;
    //---------------- Constructor ----------------
    public Outtake(HardwareMap map) {
        shooter = new Shooter(map);
        turret = new Turret(map);
        lights = new Lights(map);
    }

    //---------------- Methods ----------------

    public void setMotionSource(Supplier<Pose> pose, Supplier<Velocity> velocity) {
        this.pose = pose;
        this.velocity = velocity;
    }

    public void enableTracking(Alliance alliance) {
        aim.setAlliance(alliance);
        trackingEnabled = true;
    }

    /** Keep updating aim, RPM and hood from odometry until shooting is disabled. */
    public void enableShooting(Alliance alliance, TurretAim.Cell cell) {
        enableTracking(alliance);
        setTargetCell(cell);
        setFlywheelEnabled(true);
    }

    public void setTargetCell(TurretAim.Cell cell) {
        if (targetCell != cell) automaticFiring = false;
        targetCell = cell;
    }

    public TurretAim.Cell getTargetCell() { return targetCell; }
    public TurretAim getAim() { return aim; }

    public void setShotOffsets(double rpm, double hood, double turretDeg) {
        rpmOffset = rpm;
        hoodOffset = hood;
        turretOffsetDeg = turretDeg;
    }

    public void setFlywheelEnabled(boolean enabled) {
        flywheelEnabled = enabled;
        if (!enabled) {
            stopFiring();
            manualFiring = false;
        }
    }

    public boolean isFlywheelEnabled() { return flywheelEnabled; }
    public boolean isAtShotSpeed() { return flywheelEnabled && shooter.isShotSpeedSettled(); }
    public boolean isAutomaticFiring() { return automaticFiring; }
    public boolean isManualFiring() { return manualFiring; }
    public boolean isFiring() { return automaticFiring || manualFiring; }
    public boolean shouldRunFeedMotors() { return manualFiring || (automaticFiring && !feedPaused); }

    public void requestFire() {
        automaticRequested = true;
        pulsedFeedRequested = false;
        clearFeedRequested = false;
        feedPaused = false;
    }

    public void requestPulsedFire(boolean clearFeed) {
        automaticRequested = true;
        pulsedFeedRequested = true;
        clearFeedRequested = clearFeed;
    }

    public void stopFiring() {
        automaticRequested = false;
        pulsedFeedRequested = false;
        clearFeedRequested = false;
        automaticFiring = false;
        feedPaused = false;
    }

    public void setManualFireRequested(boolean requested) { manualRequested = requested; }

    public void stopShooting() {
        trackingEnabled = false;
        setFlywheelEnabled(false);
        manualRequested = false;
        try {
            shooter.stop();
        } finally {
            turret.stop();
        }
    }

    public boolean isReadyToFire() {
        return hasValidAutomaticShot()
                && isAtShotSpeed() && turret.isTurretNearTarget(turretToleranceDeg);
    }

    private boolean hasValidAutomaticShot() {
        return trackingEnabled && flywheelEnabled && shooter.getTargetRPM() > 0
                && aim.isInZone() && aim.getCell() == targetCell;
    }

    /** Update shot commands before Intake decides whether to feed this loop. */
    public void updateShooting(int ballCount) {
        if (!trackingEnabled) return;

        aim.update(pose.get(), velocity.get());
        double distance = aim.getDistance();
        double rpm = shooterData.getRPMVal(distance);
        double hood = shooterData.getAngleVal(distance);
        boolean valid = rpm != shooterData.INVALID && hood != shooterData.INVALID;
        shooter.setTargetRPM(flywheelEnabled && valid ? Math.max(0, rpm + rpmOffset) : 0);
        shooter.updateShotSpeedReadiness(rpmTolerance);
        if (flywheelEnabled && valid) {
            double wantedHood = hood + hoodOffset;
            if (Double.isNaN(lastHood) || Math.abs(wantedHood - lastHood) > HOOD_DEADBAND) {
                shooter.setHoodPos(wantedHood);
                lastHood = wantedHood;
            }
        }
        turret.aimAt(aim.getTurretAngle() + Math.toRadians(turretOffsetDeg));

        if (!automaticRequested || !hasValidAutomaticShot()
                || (!pulsedFeedRequested && ballCount < 1)) {
            automaticFiring = false;
            feedPaused = false;
        } else if (!automaticFiring) {
            if (ballCount > 0 && isReadyToFire()) {
                automaticFiring = true;
                feedPaused = false;
                feedPhaseTimer.reset();
            }
        } else if (pulsedFeedRequested) {
            updatePulsedFeed(ballCount);
        }
        if (!manualRequested || !flywheelEnabled || shooter.getTargetRPM() <= 0) {
            manualFiring = false;
        } else if (isAtShotSpeed()) {
            manualFiring = true;
        }

        lights.setLights(Lights.colorForBallCount(ballCount));
    }

    private void updatePulsedFeed(int ballCount) {
        if (feedPaused) {
            if ((ballCount > 0 || clearFeedRequested)
                    && feedPhaseTimer.milliseconds() >= autoFeedPauseMs && isReadyToFire()) {
                feedPaused = false;
                feedPhaseTimer.reset();
            }
        } else if (feedPhaseTimer.milliseconds() >= autoFeedPulseMs) {
            feedPaused = true;
            feedPhaseTimer.reset();
        }
    }


    //---------------- Interface Methods ----------------
    @Override
    public void init(){
        lastHood = Double.NaN;
        shooter.init();
        turret.init();
        lights.init();
    }

    @Override
    public void logPsiKitData() {

    }

    @Override
    public void update(){
        shooter.update();
        turret.update();
        lights.update();
    }

    @Override
    public void stop(){
        try {
            stopShooting();
        } finally {
            lights.stop();
        }
    }

}
