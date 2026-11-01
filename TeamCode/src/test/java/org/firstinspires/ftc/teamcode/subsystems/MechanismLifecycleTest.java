package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.Assert.*;

import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Outtake;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Shooter;
import org.junit.After;
import org.junit.Test;

/** Critical motor shutdown/restart checks with simulated hardware. */
public class MechanismLifecycleTest {
    private final Map<String, Double> powers = new HashMap<>();
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
        intake.intakeIn(0.8);
        intake.update();
        assertEquals(0.8, powers.get("spinner"), 0);
        assertEquals(0.8, powers.get("transfer"), 0);

        intake.stop();
        assertIntakeStopped(); // No update is required to stop physical output.
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
