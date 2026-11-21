package org.firstinspires.ftc.teamcode.control.outtake;

import com.bylazar.configurables.annotations.Configurable;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;
import com.qualcomm.robotcore.hardware.Gamepad;

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
 *   Y   manual shot (hold): turns the flywheel on and fires for as long as Y is held, starting as soon as
 *       the flywheel is at speed. Ignores the ball count, the zone and the turret. Letting go stops it
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
    private final Supplier<Velocity> velocity; // the robot's field velocity (in/s), for leading shots on the move

    private final ShooterData shooterData = new ShooterData();
    // Red until the OpMode picks one with setAlliance (MainTeleOp: gamepad 2 Start)
    private final TurretAim turretAim = new TurretAim(Alliance.RED);

    private boolean flywheelOn = false;   // B
    private boolean autoShoot = false;    // X
    private boolean manualHeld = false;   // Y is being held
    private boolean manualFiring = false; // Y manual shot is feeding balls
    private boolean autoFiring = false;   // auto shoot is feeding balls
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
    public OuttakeControl(Robot robot, Supplier<Pose> pose, Supplier<Velocity> velocity, Gamepad gp1, Gamepad gp2){
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
        // Y: pressing starts the manual shot, letting go stops it
        connect(() -> gp1.y, this::startManualShot, () -> manualHeld = false);
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

    // For a button that does one thing when pressed and another when let go
    private void connect(Supplier<Boolean> someGamePadControl, Runnable onPress, Runnable onRelease) {
        suppliers.add(someGamePadControl);
        consumers.add(new EdgeDetector(onPress, onRelease));
    }

    //---------------- Button Methods ----------------
    // Turning the flywheel off also cancels any shooting, so nothing gets fed into a stopped flywheel
    private void toggleFlywheel() {
        flywheelOn = !flywheelOn;
        if (!flywheelOn) {
            autoShoot = false;
            manualFiring = false;
        }
    }

    private void toggleTargetCell() {
        targetCell = targetCell == TurretAim.Cell.CLOSE ? TurretAim.Cell.FAR : TurretAim.Cell.CLOSE;
    }

    private void toggleAutoShoot() {
        autoShoot = !autoShoot;
        if (autoShoot) flywheelOn = true;
    }

    private void startManualShot() {
        flywheelOn = true;
        manualHeld = true;
    }

    /** Gamepad 2 B, and MainTeleOp's corner pose resets. */
    public void clearOffsets() {
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

        // Y manual shot: while Y is held, start feeding once the flywheel is at speed, then keep feeding until
        // Y is let go. Like auto shoot, the RPM isn't checked again once it starts, because it dips after every ball
        if (!manualHeld || !flywheelOn) {
            manualFiring = false;
        } else if (atRPM) {
            manualFiring = true;
        }

        // X auto shoot: starting needs everything lined up. Once it starts it keeps going until the balls
        // are gone, because the RPM dips after every ball and checking it again would stop the dump halfway
        // It only fires from the zone of the target Cell: in the zone AND the Cell on this half is the target
        boolean canShoot = autoShoot && spinner.getBallCount() >= 1 && turretAim.isInZone()
                && turretAim.getCell() == targetCell;
        if (!canShoot) {
            autoFiring = false;
        } else if (atRPM && turret.isTurretNearTarget(turretToleranceDeg)) {
            autoFiring = true;
        }
    }

    /** The alliance being aimed for. MainTeleOp's pose resets use it too, so both always agree. */
    public Alliance getAlliance() {
        return turretAim.getAlliance();
    }

    /** Which alliance's hive to aim at and shoot from the zone of. */
    public void setAlliance(Alliance alliance) {
        turretAim.setAlliance(alliance);
    }

    /** True while balls should be fed into the flywheel. IntakeControl opens the blocker and feeds. */
    public boolean isFiring() {
        return manualFiring || autoFiring;
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

    // Shows how full the robot is: off = empty, orange = 1 to 3 balls, green = full (4, the most allowed)
    private void updateLights(){
        lights.setLights(lightColorFor(spinner.getBallCount()));
    }

    /** Servo position for the lights at this ball count. */
    static double lightColorFor(int ballCount) {
        if (ballCount <= 0) return Lights.OFF;
        if (ballCount >= 4) return Lights.GREEN;
        return Lights.ORANGE;
    }

    private static String lightName(int ballCount) {
        if (ballCount <= 0) return "off";
        if (ballCount >= 4) return "green";
        return "orange";
    }

    //---------------- Interface Methods ----------------
    @Override
    public void update() {
        updateButtons();
        // With the velocity, TurretAim leads the shot: the turret and the RPM/hood tables use a target moved
        // against the robot's motion (TurretAim.leadEnabled turns it off in Panels)
        turretAim.update(pose.get(), velocity.get());
        updateShooter();
        updateTurret();
        updateFiring();
        updateLights();
    }

    @Override
    public void addTelemetry(Telemetry telemetry) {
        // Turret pivot to the real Cell, inches. Its own line so it's easy to read off
        telemetry.addData("Distance (in)", "%.1f", turretAim.getRealDistance());
        // While moving, the tables use the distance to the led aim point instead
        if (turretAim.isLeading()) {
            telemetry.addData("Lead", "%.2f s, aim moved %.1f in, table distance %.1f in",
                    turretAim.getFlightTime(), turretAim.getLeadOffset(), turretAim.getDistance());
        } else {
            telemetry.addData("Lead", TurretAim.leadEnabled ? "stopped" : "off (Panels)");
        }
        telemetry.addData("Flywheel", flywheelOn ? (shooter.isAtRPM(rpmTolerance) ? "READY" : "spinning up") : "off");
        telemetry.addData("Lights", "%s (%d balls)", lightName(spinner.getBallCount()), spinner.getBallCount());
        telemetry.addData("Target Cell", targetCell);
        telemetry.addData("Auto Shoot", autoShootStatus());
        telemetry.addData("Manual Shot (Y)", manualFiring ? "FIRING" : manualHeld ? "waiting: flywheel speed" : "off");
        telemetry.addData("Aim", "%s Cell, %.1f in, in zone: %b",
                turretAim.getCell(), turretAim.getRealDistance(), turretAim.isInZone());
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
