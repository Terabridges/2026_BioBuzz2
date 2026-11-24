package org.firstinspires.ftc.teamcode.control.outtake;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.intake.Spinner;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Outtake;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Shooter;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Turret;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;
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
 * Buttons send requests to Outtake. Intake handles the blocker and feed motors.
 */
@Configurable
public class OuttakeControl implements Control {

    //---------------- Tunable (Panels) ----------------
    // How much one press on gamepad 2 changes each offset
    public static double rpmOffsetStep = 25;
    public static double hoodOffsetStep = 0.01;
    public static double turretOffsetStepDeg = 1;

    //---------------- Software ----------------
    private final Outtake outtake;
    private final Shooter shooter;
    private final Turret turret;
    private final Spinner spinner;       // for the ball count
    private final TurretAim turretAim;
    private boolean autoShoot = false;    // X
    private boolean manualHeld = false;   // Y is being held

    private double rpmOffset = 0;
    private double hoodOffset = 0;
    private double turretOffsetDeg = 0; // positive = counterclockwise (left)

    private final List<Supplier<Boolean>> suppliers;
    private final List<Consumer<Boolean>> consumers;

    //---------------- Constructor ----------------
    public OuttakeControl(Outtake outtake, Spinner spinner, Gamepad gp1, Gamepad gp2){
        this.outtake = outtake;
        this.shooter = outtake.shooter;
        this.turret = outtake.turret;
        this.spinner = spinner;
        this.turretAim = outtake.getAim();

        this.suppliers = new ArrayList<>();
        this.consumers = new ArrayList<>();

        connect(() -> gp1.b, this::toggleFlywheel);
        connect(() -> gp1.x, this::toggleAutoShoot);
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
    private void toggleFlywheel() {
        outtake.setFlywheelEnabled(!outtake.isFlywheelEnabled());
        if (!outtake.isFlywheelEnabled()) autoShoot = false;
    }

    private void toggleTargetCell() {
        outtake.setTargetCell(outtake.getTargetCell() == TurretAim.Cell.CLOSE
                ? TurretAim.Cell.FAR : TurretAim.Cell.CLOSE);
    }

    private void toggleAutoShoot() {
        autoShoot = !autoShoot;
        if (autoShoot) {
            outtake.setFlywheelEnabled(true);
            outtake.requestFire();
        } else {
            outtake.stopFiring();
        }
    }

    private void startManualShot() {
        outtake.setFlywheelEnabled(true);
        manualHeld = true;
    }

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

    /** Which alliance's hive to aim at and shoot from the zone of. */
    public void setAlliance(Alliance alliance) {
        outtake.enableTracking(alliance);
    }

    private String autoShootStatus() {
        if (!autoShoot) return "off";
        if (outtake.isAutomaticFiring()) return "FIRING";
        if (spinner.getBallCount() < 1) return "waiting: no balls";
        TurretAim.Cell targetCell = outtake.getTargetCell();
        if (turretAim.getCell() != targetCell) return "waiting: on the " + turretAim.getCell() + " half, target is " + targetCell;
        if (!turretAim.isInZone()) return "waiting: not in zone";
        if (!outtake.isAtShotSpeed()) return "waiting: flywheel speed";
        return "waiting: turret";
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
        outtake.setManualFireRequested(manualHeld);
        outtake.setShotOffsets(rpmOffset, hoodOffset, turretOffsetDeg);
    }

    @Override
    public void addTelemetry(Telemetry telemetry) {
        telemetry.addData("Update Count", shooter.getUpdateCount());
        telemetry.addData("Use PID", shooter.getUseFlywheel());
        telemetry.addData("P / F", Shooter.p + " / " + Shooter.f);
        telemetry.addData("Target / Power", shooter.getTargetRPM() + " / " + shooter.getCurrentPower());
        telemetry.addData("Distance (in)", "%.1f", turretAim.getRealDistance()); // Turret pivot to the real Cell, inches.
        // While moving, the tables use the distance to the led aim point instead
        if (turretAim.isLeading()) {
            telemetry.addData("Lead", "%.2f s, aim moved %.1f in, table distance %.1f in",
                    turretAim.getFlightTime(), turretAim.getLeadOffset(), turretAim.getDistance());
        } else {
            telemetry.addData("Lead", TurretAim.leadEnabled ? "stopped" : "off (Panels)");
        }
        telemetry.addData("Flywheel", outtake.isFlywheelEnabled()
                ? (outtake.isAtShotSpeed() ? "READY" : "spinning up") : "off");
        telemetry.addData("Lights", "%s (%d balls)", lightName(spinner.getBallCount()), spinner.getBallCount());
        telemetry.addData("Target Cell", outtake.getTargetCell());
        telemetry.addData("Auto Shoot", autoShootStatus());
        telemetry.addData("Manual Shot (Y)", outtake.isManualFiring()
                ? "FIRING" : manualHeld ? "waiting: flywheel speed" : "off");
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
