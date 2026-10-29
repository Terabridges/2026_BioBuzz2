package org.firstinspires.ftc.teamcode.subsystems.drive;

import static org.junit.Assert.*;
import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.firstinspires.ftc.teamcode.control.drive.DriveControl;
import org.firstinspires.ftc.teamcode.pedro.FollowerManager;
import org.junit.After;
import org.junit.Test;

/** Catch backend sign mismatches and prevent two owners or commands after Stop. */
public class DriveBackendTest {
    private final Map<String, Double> powers = new HashMap<>();
    private final HardwareMap map = new HardwareMap(null, null) {
        @Override public <T> T get(Class<? extends T> type, String name) {
            // Direct robot-centric mode must not create a localizer or access other hardware.
            assertEquals(DcMotor.class, type);
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                    (proxy, method, args) -> {
                        if (method.getName().equals("setPower")) powers.put(name, (Double) args[0]);
                        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                        if (method.getName().equals("equals")) return proxy == args[0];
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == int.class) return 0;
                        if (method.getReturnType() == double.class) return 0.0;
                        return null;
                    }));
        }
    };

    private static class RecordingFollower extends Follower {
        DrivePowers powers;
        int updates;
        int stops;
        RecordingFollower() { super(null, null, null); }
        @Override public void manual(double f, double s, double t) { powers = new DrivePowers(f, s, t); }
        @Override public void manual(DrivePowers p) { powers = p; }
        @Override public Pose pose() { return new Pose(0, 0, Math.PI / 2); }
        @Override public void update() { updates++; }
        @Override public void stop() { stops++; }
    }

    @After public void clearHandoff() { FollowerManager.clearHandoff(); }

    @Test public void pedroConvertsRightAndClockwiseSignsWithoutUpdatingFollower() {
        RecordingFollower follower = new RecordingFollower();
        new PedroDrive(follower).robotCentric(0.4, 0.3, 0.2);
        assertEquals(0.4, follower.powers.forward(), 1e-9);
        assertEquals(-0.3, follower.powers.strafe(), 1e-9);
        assertEquals(-0.2, follower.powers.turn(), 1e-9);
        assertEquals(0, follower.updates);
    }

    @Test public void bothBackendsRotateFieldInputsInSameDirection() {
        DirectDrive direct = new DirectDrive(map);
        RecordingFollower follower = new RecordingFollower();
        new PedroDrive(follower).fieldCentric(1, 0, 0);
        direct.fieldCentricAtHeading(1, 0, 0, Math.PI / 2);
        assertEquals(0, follower.powers.forward(), 1e-9);
        assertEquals(-1, follower.powers.strafe(), 1e-9);
        // Field-forward at heading +90 degrees is robot-right.
        assertEquals(1, powers.get("front_left"), 1e-9);
        assertEquals(-1, powers.get("front_right"), 1e-9);
        assertEquals(-1, powers.get("back_left"), 1e-9);
        assertEquals(1, powers.get("back_right"), 1e-9);
    }

    @Test public void directSessionFreezesBackendAndStopsWithoutAnotherUpdate() {
        DriveManager drive = new DriveManager(DriveManager.Backend.DIRECT);
        Gamepad gamepad = new Gamepad();
        drive.start(map, false);
        DriveControl control = new DriveControl(drive.drive(), gamepad, false, false);
        gamepad.left_stick_y = -1;
        control.update();
        drive.update();
        assertEquals(1, powers.get("front_left"), 0);
        assertThrows(IllegalStateException.class, () -> drive.start(map, false));
        drive.stop();
        assertTrue(powers.values().stream().allMatch(p -> p == 0));
        drive.update(); drive.stop();
        assertThrows(IllegalStateException.class, drive::drive);
        assertTrue(powers.values().stream().allMatch(p -> p == 0));
    }

    @Test public void pedroSessionAdoptsAutoFollowerAndHasOneUpdateOwner() {
        RecordingFollower follower = new RecordingFollower();
        new FollowerManager(map, follower).handoffToTeleOp();
        DriveManager drive = new DriveManager(DriveManager.Backend.PEDRO);
        Gamepad gamepad = new Gamepad();
        drive.start(map, true);
        DriveControl control = new DriveControl(drive.drive(), gamepad, true, false);
        assertTrue(powers.isEmpty()); // No direct drivetrain was constructed.
        gamepad.left_stick_y = -1;
        control.update();
        drive.update();
        assertEquals(1, follower.updates);
        assertEquals(1, follower.powers.forward(), 0);
        int stopsBefore = follower.stops;
        drive.stop(); drive.stop(); drive.update();
        assertEquals(stopsBefore + 1, follower.stops);
        assertEquals(1, follower.updates);
    }
}
