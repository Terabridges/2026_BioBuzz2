package org.firstinspires.ftc.teamcode.utility.shooterUtility;

import static org.junit.Assert.*;

import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;
import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
import org.junit.Test;

public class MovingShotTest {
    @Test public void sidewaysShotArrivesAtTheOpeningAfterInheritingRobotVelocity() {
        Pose pose = new Pose(20, 20, 0);
        TurretAim aim = new TurretAim(Alliance.RED);
        aim.update(pose, new Velocity(15, -8, 0), distance -> 0.5);

        assertTrue(aim.isMovingLeadApplied());
        double angle = aim.getTurretAngle() + pose.heading();
        assertEquals(aim.getTargetX(), aim.getTurretX()
                + aim.getDistance() * Math.cos(angle) + 15 * 0.5, 1e-9);
        assertEquals(aim.getTargetY(), aim.getTurretY()
                + aim.getDistance() * Math.sin(angle) - 8 * 0.5, 1e-9);
    }

    @Test public void approachingUsesLessRangeAndRetreatingUsesMore() {
        Pose pose = new Pose(20, 20, 0);
        TurretAim aim = new TurretAim(Alliance.RED);
        aim.update(pose);
        double angle = aim.getTurretAngle();
        double range = aim.getDistance();
        Velocity approaching = new Velocity(10 * Math.cos(angle), 10 * Math.sin(angle), 0);

        aim.update(pose, approaching, distance -> 0.5);
        assertEquals(range - 5, aim.getDistance(), 1e-9);
        aim.update(pose, approaching.scale(-1), distance -> 0.5);
        assertEquals(range + 5, aim.getDistance(), 1e-9);
    }

    @Test public void turningAddsTheOffsetTurretsTangentialVelocity() {
        Pose pose = new Pose(20, 20, Math.PI / 2);
        TurretAim aim = new TurretAim(Alliance.RED);
        aim.update(pose, new Velocity(0, 0, 10), distance -> 0.5);

        assertTrue(aim.isMovingLeadApplied());
        double vx = -10 * (aim.getTurretY() - pose.y());
        double vy = 10 * (aim.getTurretX() - pose.x());
        double angle = aim.getTurretAngle() + pose.heading();
        assertEquals(aim.getTargetX(), aim.getTurretX()
                + aim.getDistance() * Math.cos(angle) + vx * 0.5, 1e-9);
        assertEquals(aim.getTargetY(), aim.getTurretY()
                + aim.getDistance() * Math.sin(angle) + vy * 0.5, 1e-9);
    }

    @Test public void stoppingClearsPreviousLeadAndPreservesZoneAndCellSelection() {
        Pose pose = new Pose(20, 20, 0);
        TurretAim aim = new TurretAim(Alliance.RED);
        aim.update(pose, new Velocity(15, 0, 0), distance -> 0.5);
        assertTrue(aim.isMovingLeadApplied());
        assertEquals(TurretAim.Cell.CLOSE, aim.getCell());
        assertTrue(aim.isInZone());

        aim.update(pose, Velocity.zero(), distance -> { throw new AssertionError(); });
        assertFalse(aim.isMovingLeadApplied());
        assertEquals(aim.getGeometricDistance(), aim.getDistance(), 0);
        assertEquals(0, aim.getShotFlightTime(), 0);
    }

    @Test public void thresholdAndMissingOrInvalidVelocityUseGeometricAim() {
        Pose pose = new Pose(20, 20, 0);
        TurretAim aim = new TurretAim(Alliance.RED);
        for (Velocity velocity : new Velocity[]{null, new Velocity(2, 0, 0),
                new Velocity(Double.NaN, 10, 0)}) {
            aim.update(pose, velocity, distance -> { throw new AssertionError(); });
            assertFalse(aim.isMovingLeadApplied());
            assertEquals(aim.getGeometricDistance(), aim.getDistance(), 0);
        }
    }

    @Test public void invalidFlightTimeCannotLeaveAPartiallyCompensatedAim() {
        TurretAim aim = new TurretAim(Alliance.RED);
        aim.update(new Pose(20, 20, 0), new Velocity(15, 0, 0), distance -> Double.NaN);
        assertFalse(aim.isMovingLeadApplied());
        assertEquals(aim.getGeometricDistance(), aim.getDistance(), 0);
    }

    @Test public void variableFlightTimeConvergesToAnIntercept() {
        TurretAim aim = new TurretAim(Alliance.RED);
        Pose pose = new Pose(20, 20, 0);
        aim.update(pose, new Velocity(10, 0, 0), distance -> distance / 100);
        assertEquals(aim.getDistance() / 100, aim.getShotFlightTime(), 1e-8);
    }
}
