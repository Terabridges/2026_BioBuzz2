package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.Assert.*;

import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.firstinspires.ftc.teamcode.actions.Action;
import org.firstinspires.ftc.teamcode.actions.CollectAction;
import org.firstinspires.ftc.teamcode.actions.ShootAction;
import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
import org.firstinspires.ftc.teamcode.autonomous.paths.FieldPoses;
import org.firstinspires.ftc.teamcode.control.outtake.OuttakeControl;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Outtake;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Shooter;
import org.firstinspires.ftc.teamcode.utility.shooterUtility.TurretAim;
import org.junit.After;
import org.junit.Test;

/** Critical motor shutdown/restart checks with simulated hardware. */
public class MechanismLifecycleTest {
    private final Map<String, Double> powers = new HashMap<>();
    private final Map<String, Double> servoPositions = new HashMap<>();
    private double flywheelVelocity;
    private String failingMotor;
    private final HardwareMap map = new HardwareMap(null, null) {
        @Override public <T> T get(Class<? extends T> type, String name) {
            if (type == AnalogInput.class) return type.cast(new AnalogInput(null, 0));
            Class<?>[] interfaces = type == Servo.class
                    ? new Class<?>[]{Servo.class, PwmControl.class} : new Class<?>[]{type};
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), interfaces,
                    (proxy, method, args) -> {
                        if (method.getName().equals("setPower")) {
                            if (name.equals(failingMotor)) throw new IllegalStateException("Motor failure");
                            powers.put(name, (Double) args[0]);
                        }
                        if (method.getName().equals("setPosition")) {
                            servoPositions.put(name, (Double) args[0]);
                        }
                        if (method.getName().equals("getVelocity") && name.equals("flywheel")) {
                            return flywheelVelocity;
                        }
                        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                        if (method.getName().equals("equals")) return proxy == args[0];
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == int.class) return 0;
                        if (method.getReturnType() == double.class) return 0.0;
                        return null;
                    }));
        }
    };

    @After public void resetTarget() { Shooter.flywheelTargetRPM = 0; }

    @Test public void intakeStopImmediatelyZerosBothMotorsAndClearsCommands() {
        Intake intake = new Intake(map);
        intake.init();
        double closedPosition = servoPositions.get("blocker");
        intake.transfer.openBlocker();
        intake.intakeIn(0.8);
        intake.update();
        assertEquals(0.8, powers.get("spinner"), 0);
        assertEquals(0.8, powers.get("transfer"), 0);
        assertNotEquals(closedPosition, servoPositions.get("blocker"), 0);

        intake.stop();
        assertIntakeStopped(); // No update is required to stop physical output.
        assertEquals(closedPosition, servoPositions.get("blocker"), 0);
        assertFalse(intake.transfer.isBlockerOpen());
        intake.update();
        assertIntakeStopped(); // An old command cannot restart the motors.
    }

    @Test public void outtakeStopZerosFlywheelsAndCancelsTurretMotion() {
        Outtake outtake = new Outtake(map);
        outtake.init();
        outtake.shooter.setTargetRPM(2000);
        outtake.turret.turnTurretLeft();
        outtake.update();
        assertTrue(powers.get("flywheel") > 0);
        assertFalse(outtake.turret.isTurretAtTarget());

        outtake.stop();
        assertShooterStopped(outtake.shooter);
        assertTrue(outtake.turret.isTurretAtTarget());
        outtake.update();
        assertShooterStopped(outtake.shooter);
    }

    @Test public void shooterConstructionAndInitDiscardOldTunerTarget() {
        Shooter.flywheelTargetRPM = 2500; // Previous OpMode exited with a live static target.
        Shooter shooter = new Shooter(map);
        assertShooterStopped(shooter);
        shooter.setTargetRPM(2000);
        shooter.update();
        shooter.init();
        assertShooterStopped(shooter);
        shooter.update();
        assertShooterStopped(shooter);
    }

    @Test public void intakeStillStopsTransferWhenSpinnerStopFails() {
        Intake intake = new Intake(map);
        intake.intakeIn(0.8);
        intake.update();
        failingMotor = "spinner";
        assertThrows(IllegalStateException.class, intake::stop);
        assertEquals(0, powers.get("transfer"), 0);
    }

    @Test public void outtakeStillStopsTurretWhenFlywheelMotorFails() {
        Outtake outtake = new Outtake(map);
        outtake.init();
        outtake.shooter.setTargetRPM(2000);
        outtake.turret.turnTurretLeft();
        outtake.update();
        failingMotor = "flywheel";
        assertThrows(IllegalStateException.class, outtake::stop);
        assertEquals(0, outtake.shooter.getTargetRPM(), 0);
        assertTrue(outtake.turret.isTurretAtTarget());
    }

    @Test public void shootingWaitsForPermissionAndReadinessThenFeedsThroughRpmDip() {
        Intake intake = new Intake(map);
        intake.init();
        double closedPosition = servoPositions.get("blocker");
        Outtake outtake = preparedOuttake();
        boolean[] settled = {false};
        ShootAction shot = new ShootAction(outtake, intake, () -> settled[0], 5);
        shot.start();

        shot.update();
        outtake.updateShooting(1);
        assertTrue(outtake.shooter.getTargetRPM() > 0);
        assertFalse(outtake.isFiring());

        settled[0] = true;
        shot.update();
        outtake.updateShooting(1);
        assertFalse(outtake.isFiring()); // Flywheel is still at zero measured RPM.

        flywheelVelocity = outtake.shooter.RPMToVel(outtake.shooter.getTargetRPM());
        outtake.shooter.update();
        outtake.updateShooting(1);
        assertTrue(outtake.isFiring());
        intake.updateCommands(outtake.isFiring());
        intake.update();
        assertNotEquals(closedPosition, servoPositions.get("blocker"), 0);

        flywheelVelocity = 0;
        outtake.shooter.update();
        outtake.updateShooting(1);
        assertTrue(outtake.isFiring());
        shot.stop();
        assertEquals(Action.Result.CANCELLED, shot.result());
        assertFalse(outtake.isFiring());
        assertIntakeStopped();
        assertEquals(closedPosition, servoPositions.get("blocker"), 0);
    }

    @Test public void shotTimeoutStopsTheFeedWithoutAnotherHardwareUpdate() throws InterruptedException {
        Intake intake = new Intake(map);
        intake.init();
        double closedPosition = servoPositions.get("blocker");
        intake.updateCommands(true);
        intake.update();
        ShootAction shot = new ShootAction(preparedOuttake(), intake, () -> false, 0.001);
        shot.start();
        Thread.sleep(10);
        shot.update();

        assertEquals(Action.Result.TIMED_OUT, shot.result());
        assertIntakeStopped();
        assertEquals(closedPosition, servoPositions.get("blocker"), 0);
    }

    @Test public void shootingFinishesAfterConfirmedEmptyAndKeepsSpinupEnabled() throws InterruptedException {
        Intake intake = new Intake(map);
        intake.init();
        Outtake outtake = preparedOuttake();
        ShootAction shot = new ShootAction(outtake, intake, () -> true, 5);
        shot.start();
        shot.update();
        assertFalse(shot.isFinished()); // Empty before feeding is not a completed shot.
        intake.spinner.setBallCount(1);
        outtake.updateShooting(1);
        flywheelVelocity = outtake.shooter.RPMToVel(outtake.shooter.getTargetRPM());
        outtake.shooter.update();
        outtake.updateShooting(1);
        shot.update();
        assertTrue(outtake.isFiring());

        intake.spinner.setBallCount(0);
        shot.update();
        assertFalse(shot.isFinished());
        Thread.sleep(225);
        shot.update();

        assertEquals(Action.Result.SUCCEEDED, shot.result());
        assertFalse(outtake.isFiring());
        assertTrue(outtake.isFlywheelEnabled());
    }

    @Test public void collectionUsesTheSharedIntakeAndStopsAtItsRequestedCount() {
        Intake intake = new Intake(map);
        intake.init();
        CollectAction collect = new CollectAction(intake, 2, 5);
        collect.start();
        intake.updateCommands(false);
        intake.update();
        assertTrue(powers.get("spinner") > 0);
        intake.spinner.setBallCount(2);
        collect.update();
        intake.updateCommands(false);
        intake.update();
        assertEquals(Action.Result.SUCCEEDED, collect.result());
        assertIntakeStopped();
    }

    @Test public void teleopManualShotFeedsOnlyWhileHeldAndEjectTakesPriority() {
        Intake intake = new Intake(map);
        intake.init();
        Outtake outtake = preparedOuttake();
        outtake.setFlywheelEnabled(false);
        Gamepad driver = new Gamepad();
        OuttakeControl control = new OuttakeControl(outtake, intake.spinner, driver, new Gamepad());

        driver.y = true;
        control.update();
        outtake.updateShooting(0);
        assertTrue(outtake.isFlywheelEnabled());
        assertFalse(outtake.isFiring());

        flywheelVelocity = outtake.shooter.RPMToVel(outtake.shooter.getTargetRPM());
        outtake.shooter.update();
        outtake.updateShooting(0);
        assertTrue(outtake.isManualFiring()); // Manual feed ignores the ball-count gate.
        intake.eject();
        intake.updateCommands(outtake.isFiring());
        intake.update();
        assertTrue(powers.get("spinner") < 0);
        assertFalse(intake.transfer.isBlockerOpen());

        driver.y = false;
        control.update();
        outtake.updateShooting(0);
        assertFalse(outtake.isFiring());
        assertTrue(outtake.isFlywheelEnabled());
    }

    private Outtake preparedOuttake() {
        Outtake outtake = new Outtake(map);
        outtake.init();
        Pose corner = FieldPoses.AUDIENCE_LEFT_CORNER_START;
        double angleToCell = Math.atan2(TurretAim.redCloseCellY - corner.y(),
                TurretAim.redCloseCellX - corner.x());
        // The turret starts pointed backward. Give the simulated robot a heading already aligned.
        Pose aligned = new Pose(corner.x(), corner.y(), angleToCell + Math.PI);
        outtake.setMotionSource(() -> aligned, Velocity::zero);
        outtake.enableShooting(Alliance.RED, TurretAim.Cell.CLOSE);
        outtake.updateShooting(1);
        assertTrue(outtake.turret.isTurretNearTarget(Outtake.turretToleranceDeg));
        return outtake;
    }

    private void assertIntakeStopped() {
        assertEquals(0, powers.get("spinner"), 0);
        assertEquals(0, powers.get("transfer"), 0);
    }

    private void assertShooterStopped(Shooter shooter) {
        assertEquals(0, powers.get("flywheel"), 0);
        assertEquals(0, shooter.getTargetRPM(), 0);
        assertFalse(shooter.isShooting());
    }
}
