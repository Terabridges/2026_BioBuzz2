package org.firstinspires.ftc.teamcode.control.outtake;

import com.bylazar.configurables.annotations.Configurable;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.subsystems.intake.Spinner;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Lights;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Shooter;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Turret;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;
import org.firstinspires.ftc.teamcode.utility.shooterUtility.ShooterData;
import org.firstinspires.ftc.teamcode.utility.shooterUtility.TurretAim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.psilynx.psikit.core.Logger;

/**
 * Aims and shoots. The turret always tracks the Cell from odometry, and while the flywheel is on its RPM and
 * the hood follow the shot tables for the current distance.
 *
 * <pre>
 * Gamepad 1
 *   B   toggle flywheel
 *   X   toggle auto shoot: turns the flywheel on, then fires whenever there's a ball, the robot is in the
 *       zone of the target Cell, the turret is on target and the flywheel is at speed
 *   Y   shoot macro: turns the flywheel on, waits for speed, then fires no matter what
 *   D-pad down   switch the target Cell (the one that's open): close (y < 72) or far. Starts on close
 * Gamepad 2 (offsets for when shots land a bit off)
 *   D-pad up / down      hood offset
 *   D-pad left / right   turret offset
 *   RB / LB              RPM offset up / down
 *   B                    clear offsets
 * </pre>
 *
 * This class only decides when to fire. IntakeControl owns the blocker and the feed motors: it asks
 * {@link #isFiring()} every loop.
 */
@Configurable
public class OuttakeControl implements Control {

    //---------------- Tunable (Panels) ----------------
    public static double rpmTolerance = 50;       // RPM from target before a shot can start
    public static double turretToleranceDeg = 3;  // turret degrees from its aim before an auto shot can start
    public static double shootMacroMs = 1500;     // how long the Y macro feeds balls
    // How much one press on gamepad 2 changes each offset
    public static double rpmOffsetStep = 25;
    public static double hoodOffsetStep = 0.01;
    public static double turretOffsetStepDeg = 1;

    //---------------- Software ----------------
    private final Shooter shooter;
    private final Turret turret;
    private final Lights lights;
    private final Spinner spinner;       // for the ball count
    private final Supplier<Pose> pose;   // the robot's field pose, from odometry
    private final Supplier<Velocity> velocity;

    private final ShooterData shooterData = new ShooterData();
    // TODO pick the alliance (from autonomous, or during init). Red only for now
    private final TurretAim turretAim = new TurretAim(Alliance.RED);

    private boolean flywheelOn = false;   // B
    private boolean autoShoot = false;    // X
    private boolean macroWaiting = false; // Y was pressed, waiting for the flywheel to reach speed
    private boolean macroFiring = false;  // Y macro is feeding balls
    private boolean autoFiring = false;   // auto shoot is feeding balls
    private final ElapsedTime macroTimer = new ElapsedTime();
    // The Cell to shoot at, picked by the driver because the robot can't tell which one the hive has tipped
    // open. The turret still aims at the Cell on the robot's own half; this only decides whether auto shoot
    // may fire there (D-pad down)
    private TurretAim.Cell targetCell = TurretAim.Cell.CLOSE;

    private double rpmOffset = 0;
    private double hoodOffset = 0;
    private double turretOffsetDeg = 0; // positive = counterclockwise (left)

    // The table's hood value changes a tiny bit every time odometry moves. Only send it to the servo when it
    // has moved more than this, so the servo isn't rewritten every loop
    private static final double HOOD_DEADBAND = 0.002;
    private double lastHood = Double.NaN; // last hood position asked for

    private final List<Supplier<Boolean>> suppliers;
    private final List<Consumer<Boolean>> consumers;

    //---------------- Constructor ----------------
    public OuttakeControl(Robot robot, Supplier<Pose> pose, Gamepad gp1, Gamepad gp2){
        this(robot, pose, Velocity::zero, gp1, gp2);
    }

    public OuttakeControl(Robot robot, Supplier<Pose> pose, Supplier<Velocity> velocity,
                          Gamepad gp1, Gamepad gp2) {
        this.shooter = robot.outtake.shooter;
        this.turret = robot.outtake.turret;
        this.lights = robot.outtake.lights;
        this.spinner = robot.intake.spinner;
        this.pose = pose;
        this.velocity = velocity;

        this.suppliers = new ArrayList<>();
        this.consumers = new ArrayList<>();

        connect(() -> gp1.b, this::toggleFlywheel);
        connect(() -> gp1.x, this::toggleAutoShoot);
        connect(() -> gp1.y, this::startShootMacro);
        connect(() -> gp1.dpad_down, this::toggleTargetCell);

        connect(() -> gp2.dpad_up, () -> hoodOffset += hoodOffsetStep);
        connect(() -> gp2.dpad_down, () -> hoodOffset -= hoodOffsetStep);
        connect(() -> gp2.dpad_left, () -> turretOffsetDeg += turretOffsetStepDeg);
        connect(() -> gp2.dpad_right, () -> turretOffsetDeg -= turretOffsetStepDeg);
        connect(() -> gp2.right_bumper, () -> rpmOffset += rpmOffsetStep);
        connect(() -> gp2.left_bumper, () -> rpmOffset -= rpmOffsetStep);
        connect(() -> gp2.b, this::clearOffsets);
    }

    private void connect(Supplier<Boolean> someGamePadControl, Runnable thingToDo) {
        suppliers.add(someGamePadControl);
        consumers.add(new EdgeDetector(thingToDo));
    }

    //---------------- Button Methods ----------------
    // Turning the flywheel off also cancels any shooting, so nothing gets fed into a stopped flywheel
    private void toggleFlywheel() {
        flywheelOn = !flywheelOn;
        if (!flywheelOn) {
            autoShoot = false;
            macroWaiting = false;
            macroFiring = false;
        }
    }

    private void toggleTargetCell() {
        targetCell = targetCell == TurretAim.Cell.CLOSE ? TurretAim.Cell.FAR : TurretAim.Cell.CLOSE;
    }

    private void toggleAutoShoot() {
        autoShoot = !autoShoot;
        if (autoShoot) flywheelOn = true;
    }

    private void startShootMacro() {
        flywheelOn = true;
        if (!macroFiring) macroWaiting = true;
    }

    private void clearOffsets() {
        rpmOffset = 0;
        hoodOffset = 0;
        turretOffsetDeg = 0;
    }

    //---------------- Methods ----------------
    private void updateButtons() {
        for (int i = 0; i < suppliers.size(); i++) {
            Supplier<Boolean> oneSupplier = suppliers.get(i);
            Consumer<Boolean> oneConsumer = consumers.get(i);
            oneConsumer.accept(oneSupplier.get());
        }
    }

    // RPM and hood from the tables for the current distance, plus the gamepad 2 offsets
    private void updateShooter() {
        double distance = turretAim.getDistance();
        double rpm = shooterData.getRPMVal(distance);
        double hood = shooterData.getAngleVal(distance);
        boolean valid = rpm != shooterData.INVALID && hood != shooterData.INVALID;

        shooter.setTargetRPM(flywheelOn && valid ? rpm + rpmOffset : 0);

        if (flywheelOn && valid) {
            double wantedHood = hood + hoodOffset;
            if (Double.isNaN(lastHood) || Math.abs(wantedHood - lastHood) > HOOD_DEADBAND) {
                shooter.setHoodPos(wantedHood);
                lastHood = wantedHood;
            }
        }
    }

    // The turret tracks the Cell all the time, so it's already there when it's time to shoot
    private void updateTurret() {
        turret.aimAt(turretAim.getTurretAngle() + Math.toRadians(turretOffsetDeg));
    }

    private void updateFiring() {
        boolean atRPM = shooter.isAtRPM(rpmTolerance);

        // Y macro: once the flywheel is at speed, feed for shootMacroMs no matter what
        if (macroWaiting && atRPM) {
            macroWaiting = false;
            macroFiring = true;
            macroTimer.reset();
        }
        if (macroFiring && macroTimer.milliseconds() >= shootMacroMs) {
            macroFiring = false;
        }

        // X auto shoot: starting needs everything lined up. Once it starts it keeps going until the balls
        // are gone, because RPM dips after every ball. Turret tracking is still checked on every loop.
        // It only fires from the zone of the target Cell: in the zone AND the Cell on this half is the target
        boolean canShoot = autoShoot && spinner.getBallCount() >= 1 && turretAim.isInZone()
                && turretAim.getCell() == targetCell && shooter.getTargetRPM() > 0
                && turret.isTurretNearTarget(turretToleranceDeg);
        if (!canShoot) {
            autoFiring = false;
        } else if (atRPM) {
            autoFiring = true;
        }
    }

    /** True while balls should be fed into the flywheel. IntakeControl opens the blocker and feeds. */
    public boolean isFiring() {
        return macroFiring || autoFiring;
    }

    // Says what auto shoot is waiting on, so the drivers can tell why it isn't firing
    private String autoShootStatus() {
        if (!autoShoot) return "off";
        if (autoFiring) return "FIRING";
        if (spinner.getBallCount() < 1) return "waiting: no balls";
        if (turretAim.getCell() != targetCell) return "waiting: on the " + turretAim.getCell() + " half, target is " + targetCell;
        if (!turretAim.isInZone()) return "waiting: not in zone";
        if (!shooter.isAtRPM(rpmTolerance)) return "waiting: flywheel speed";
        return "waiting: turret";
    }

    private void updateLights(){

    }

    //---------------- Interface Methods ----------------
    @Override
    public void update() {
        updateButtons();
        turretAim.update(pose.get(), velocity.get(), shooterData::getShotTimeVal);
        updateShooter();
        updateTurret();
        updateFiring();
        updateLights();
    }

    @Override
    public void addTelemetry(Telemetry telemetry) {
        telemetry.addData("Flywheel", flywheelOn ? (shooter.isAtRPM(rpmTolerance) ? "READY" : "spinning up") : "off");
        telemetry.addData("Target Cell", targetCell);
        telemetry.addData("Auto Shoot", autoShootStatus());
        telemetry.addData("Shoot Macro", macroFiring ? "FIRING" : macroWaiting ? "waiting: flywheel speed" : "off");
        telemetry.addData("Aim", "%s Cell, %.1f in, in zone: %b",
                turretAim.getCell(), turretAim.getDistance(), turretAim.isInZone());
        telemetry.addData("Moving lead", "%b, flight %.3f s, geometric / shot range %.1f / %.1f in",
                turretAim.isMovingLeadApplied(), turretAim.getShotFlightTime(),
                turretAim.getGeometricDistance(), turretAim.getDistance());
        telemetry.addData("RPM target / current", "%.0f / %.0f", shooter.getTargetRPM(), shooter.getCurrentRPM());
        telemetry.addData("Hood Pos", "%.3f", shooter.getHoodPos());
        telemetry.addData("Turret", "%.1f deg from front, pos %.3f",
                Math.toDegrees(turretAim.getTurretAngle()), turret.getTurretPos());
        telemetry.addData("Offsets RPM / hood / turret", "%+.0f / %+.3f / %+.1f deg",
                rpmOffset, hoodOffset, turretOffsetDeg);
        Logger.recordOutput("Shooter Target RPM", shooter.getTargetRPM());
        Logger.recordOutput("Shooter Current RPM", shooter.getCurrentRPM());
        Logger.recordOutput("Raw Flywheel Vel", shooter.getFlywheelVel());
    }
}
