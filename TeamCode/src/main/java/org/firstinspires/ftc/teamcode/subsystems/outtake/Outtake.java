package org.firstinspires.ftc.teamcode.subsystems.outtake;

import com.bylazar.configurables.annotations.Configurable;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;
import com.qualcomm.robotcore.hardware.HardwareMap;

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
    private static final double HOOD_DEADBAND = 0.002;

    private final ShooterData shooterData = new ShooterData();
    private final TurretAim aim = new TurretAim(Alliance.RED);
    private Supplier<Pose> pose;
    private Supplier<Velocity> velocity;
    private TurretAim.Cell targetCell = TurretAim.Cell.CLOSE;
    private boolean trackingEnabled;
    private boolean flywheelEnabled;
    private boolean automaticRequested;
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
    public boolean isAtShotSpeed() { return shooter.isAtRPM(rpmTolerance); }
    public boolean isAutomaticFiring() { return automaticFiring; }
    public boolean isManualFiring() { return manualFiring; }
    public boolean isFiring() { return automaticFiring || manualFiring; }

    /** Allow automatic feeding once the shot is ready. Intake applies the feed request. */
    public void requestFire() { automaticRequested = true; }

    public void stopFiring() {
        automaticRequested = false;
        automaticFiring = false;
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

        // RPM normally dips during feeding. Check it again only when starting another dump.
        if (!automaticRequested || ballCount < 1 || !hasValidAutomaticShot()) {
            automaticFiring = false;
        } else if (isReadyToFire()) {
            automaticFiring = true;
        }
        if (!manualRequested || !flywheelEnabled || shooter.getTargetRPM() <= 0) {
            manualFiring = false;
        } else if (isAtShotSpeed()) {
            manualFiring = true;
        }

        lights.setLights(Lights.colorForBallCount(ballCount));
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
