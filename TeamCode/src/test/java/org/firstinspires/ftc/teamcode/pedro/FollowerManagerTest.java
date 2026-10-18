package org.firstinspires.ftc.teamcode.pedro;

import static org.junit.Assert.*;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Critical ownership tests; all followers and motors are simulated on the computer. */
public class FollowerManagerTest {
    private static class TestFollower extends Follower {
        Pose pose = new Pose(45, 72, 1.2);
        int updates;
        int stops;
        int manualCommands;
        int poseResets;

        TestFollower() { super(null, null, null); }
        TestFollower(Mecanum drivetrain) { super(null, drivetrain, null); }
        @Override public void update() { updates++; }
        @Override public void stop() { stops++; }
        @Override public Pose pose() { return pose; }
        @Override public void setPose(Pose pose) { poseResets++; this.pose = pose; }
        @Override public void manual(double forward, double lateral, double turn) {
            assertEquals(0, forward, 0);
            assertEquals(0, lateral, 0);
            assertEquals(0, turn, 0);
            manualCommands++;
        }
    }

    @Before public void clearBefore() { FollowerManager.clearHandoff(); }
    @After public void clearAfter() { FollowerManager.clearHandoff(); }

    @Test public void handoffTransfersSameFollowerAndPoseOnlyOnce() {
        TestFollower follower = new TestFollower();
        Pose originalPose = follower.pose();
        FollowerManager auto = new FollowerManager(null, follower);
        auto.update();
        auto.handoffToTeleOp();
        int stopsAtHandoff = follower.stops;
        auto.update();
        auto.stop();
        assertEquals(1, follower.updates);
        assertEquals(stopsAtHandoff, follower.stops);

        FollowerManager teleop = FollowerManager.forTeleOp(null, () -> {
            throw new AssertionError("A handoff should not create a new follower");
        });
        assertTrue(teleop.resumedFromAuto());
        assertSame(follower, teleop.follower());
        assertSame(originalPose, follower.pose());
        assertEquals(0, follower.poseResets);
        assertEquals(0, follower.manualCommands); // Init remains stopped.
        teleop.startManual();
        assertEquals(1, follower.manualCommands);
        teleop.update();
        assertEquals(2, follower.updates);

        TestFollower fresh = new TestFollower();
        FollowerManager anotherTeleop = FollowerManager.forTeleOp(null, () -> fresh);
        assertFalse(anotherTeleop.resumedFromAuto());
        assertSame(fresh, anotherTeleop.follower());
    }

    @Test public void standaloneTeleopUsesFreshFollowerAndStopIsIdempotent() {
        TestFollower fresh = new TestFollower();
        FollowerManager manager = FollowerManager.forTeleOp(null, () -> fresh);
        assertFalse(manager.resumedFromAuto());
        manager.update();
        manager.stop();
        int stops = fresh.stops;
        manager.stop();
        manager.update();
        assertEquals(stops, fresh.stops);
        assertEquals(1, fresh.updates);
    }

    @Test public void clearingHandoffPreventsReusingAnOldAuto() {
        new FollowerManager(null, new TestFollower()).handoffToTeleOp();
        FollowerManager.clearHandoff();
        TestFollower fresh = new TestFollower();
        assertSame(fresh, FollowerManager.forTeleOp(null, () -> fresh).follower());
    }

    @Test public void changedHardwareMapRejectsHandoff() {
        HardwareMap oldMap = new HardwareMap(null, null);
        HardwareMap newMap = new HardwareMap(null, null);
        new FollowerManager(oldMap, new TestFollower()).handoffToTeleOp();
        TestFollower fresh = new TestFollower();
        FollowerManager manager = FollowerManager.forTeleOp(newMap, () -> fresh);
        assertFalse(manager.resumedFromAuto());
        assertSame(fresh, manager.follower());
    }

    @Test(expected = IllegalStateException.class)
    public void releasedOwnerCannotRetrieveFollowerAgain() {
        FollowerManager manager = new FollowerManager(null, new TestFollower());
        manager.handoffToTeleOp();
        manager.follower();
    }

    @Test public void handoffRestoresDirectionsAndBrakingAfterSdkReset() {
        Map<String, DcMotorEx> motors = new HashMap<>();
        HardwareMap map = new HardwareMap(null, null) {
            @Override public <T> T get(Class<? extends T> type, String name) {
                return type.cast(motors.get(name));
            }
        };
        Map<String, DcMotorSimple.Direction> directions = new HashMap<>();
        Map<String, DcMotor.ZeroPowerBehavior> braking = new HashMap<>();
        for (String name : new String[]{"fl", "fr", "bl", "br"}) {
            DcMotorEx motor = (DcMotorEx) Proxy.newProxyInstance(DcMotorEx.class.getClassLoader(),
                    new Class<?>[]{DcMotorEx.class}, (proxy, method, args) -> {
                        if (method.getName().equals("setDirection")) directions.put(name, (DcMotorSimple.Direction) args[0]);
                        if (method.getName().equals("setZeroPowerBehavior")) braking.put(name, (DcMotor.ZeroPowerBehavior) args[0]);
                        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                        if (method.getName().equals("equals")) return proxy == args[0];
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == int.class) return 0;
                        if (method.getReturnType() == double.class) return 0.0;
                        return null;
                    });
            motors.put(name, motor);
        }
        MecanumConfig config = new MecanumConfig(c -> {
            c.frontLeftName.set("fl"); c.frontRightName.set("fr");
            c.backLeftName.set("bl"); c.backRightName.set("br");
            c.frontLeftDirection.set(DcMotorSimple.Direction.FORWARD);
            c.backLeftDirection.set(DcMotorSimple.Direction.FORWARD);
            c.frontRightDirection.set(DcMotorSimple.Direction.REVERSE);
            c.backRightDirection.set(DcMotorSimple.Direction.REVERSE);
            c.manualBrakeMode.set(true);
        });
        TestFollower follower = new TestFollower(new Mecanum(map, config));
        new FollowerManager(map, follower).handoffToTeleOp();
        // Simulate the SDK resetting the raw motors between auto and TeleOp.
        directions.replaceAll((name, direction) -> DcMotorSimple.Direction.FORWARD);
        braking.replaceAll((name, behavior) -> DcMotor.ZeroPowerBehavior.FLOAT);
        FollowerManager.forTeleOp(map, () -> {
            throw new AssertionError("Must keep the actual follower");
        });
        assertEquals(DcMotorSimple.Direction.FORWARD, directions.get("fl"));
        assertEquals(DcMotorSimple.Direction.REVERSE, directions.get("fr"));
        assertEquals(DcMotorSimple.Direction.FORWARD, directions.get("bl"));
        assertEquals(DcMotorSimple.Direction.REVERSE, directions.get("br"));
        assertTrue(braking.values().stream().allMatch(value -> value == DcMotor.ZeroPowerBehavior.BRAKE));
        assertEquals(0, follower.poseResets);
    }
}
