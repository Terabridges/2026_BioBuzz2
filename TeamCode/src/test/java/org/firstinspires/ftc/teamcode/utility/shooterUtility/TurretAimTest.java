package org.firstinspires.ftc.teamcode.utility.shooterUtility;

import static org.junit.Assert.*;

import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
import org.junit.After;
import org.junit.Test;

/** The shooting zone: two rows of tiles behind the Cell being aimed at, five columns from the alliance's side wall. */
public class TurretAimTest {
    private static boolean inZone(Alliance alliance, double x, double y) {
        TurretAim aim = new TurretAim(alliance); // a new one each time, so the Cell comes from this y alone
        aim.update(x, y, 0);
        return aim.isInZone();
    }

    @Test public void redCloseZoneIsFromTheAudienceWallToRowTwoAndTheLeftWallToColumnFive() {
        assertTrue(inZone(Alliance.RED, 8, 8));
        assertTrue(inZone(Alliance.RED, 115, 40));
        assertFalse(inZone(Alliance.RED, 125, 40)); // sixth column
        assertFalse(inZone(Alliance.RED, 48, 60));  // third row
    }

    @Test public void farCellZoneIsTheSameThingFromTheFarWall() {
        assertTrue(inZone(Alliance.RED, 48, 120));
        assertFalse(inZone(Alliance.RED, 48, 90));
        assertFalse(inZone(Alliance.RED, 125, 120));
    }

    @Test public void blueZoneIsRedMovedOverOneColumn() {
        assertTrue(inZone(Alliance.BLUE, 136, 8));
        assertTrue(inZone(Alliance.BLUE, 30, 40));
        assertFalse(inZone(Alliance.BLUE, 20, 40)); // first column
    }

    //---------------- Shooting on the move ----------------
    // Lead settings are static (Panels), so put them back after every test
    @After public void resetLeadSettings() {
        TurretAim.leadEnabled = true;
        TurretAim.leadScale = 1.0;
        TurretAim.leadMinSpeed = 2;
        TurretAim.leadIterations = 5;
    }

    // Robot straight back from the red close Cell, facing it (heading 90 deg = +y)
    private static final double X = TurretAim.redCloseCellX, Y = 20, HEADING = Math.PI / 2;

    @Test public void stoppedRobotAimsStraightAtTheCell() {
        TurretAim still = new TurretAim(Alliance.RED);
        still.update(X, Y, HEADING);
        TurretAim zeroVelocity = new TurretAim(Alliance.RED);
        zeroVelocity.update(X, Y, HEADING, 0, 0);

        assertFalse(zeroVelocity.isLeading());
        assertEquals(still.getTurretAngle(), zeroVelocity.getTurretAngle(), 1e-9);
        assertEquals(zeroVelocity.getRealDistance(), zeroVelocity.getDistance(), 1e-9);
    }

    @Test public void slowerThanLeadMinSpeedCountsAsStopped() {
        TurretAim aim = new TurretAim(Alliance.RED);
        aim.update(X, Y, HEADING, 1, 1); // about 1.4 in/s
        assertFalse(aim.isLeading());
        assertEquals(0, aim.getLeadOffset(), 1e-9);
    }

    @Test public void drivingRightAimsLeftOfTheCellByVelocityTimesFlightTime() {
        TurretAim aim = new TurretAim(Alliance.RED);
        aim.update(X, Y, HEADING, 40, 0); // strafing right (+x) at 40 in/s

        assertTrue(aim.isLeading());
        assertTrue("aim point is left of the Cell", aim.getAimX() < aim.getTargetX());
        assertEquals(aim.getTargetY(), aim.getAimY(), 1e-9);
        assertTrue("turret turns left (counterclockwise)", aim.getTurretAngle() > 0);
        // Settled: the lead matches the table's flight time for the distance it ends up using
        double t = new ShooterData().getShotTimeVal(aim.getDistance());
        assertEquals(40 * t, aim.getTargetX() - aim.getAimX(), 0.05);
        assertEquals(t, aim.getFlightTime(), 0.01);
    }

    @Test public void drivingTowardTheCellShortensTheTableDistance() {
        TurretAim aim = new TurretAim(Alliance.RED);
        aim.update(X, Y, HEADING, 0, 30); // driving toward the Cell
        assertTrue(aim.getDistance() < aim.getRealDistance());
        assertEquals(0, aim.getTurretAngle(), 1e-6); // still straight ahead
    }

    @Test public void leadScaleMultipliesTheFlightTime() {
        TurretAim normal = new TurretAim(Alliance.RED);
        normal.update(X, Y, HEADING, 40, 0);
        TurretAim.leadScale = 0.5;
        TurretAim half = new TurretAim(Alliance.RED);
        half.update(X, Y, HEADING, 40, 0);
        assertTrue(half.getLeadOffset() < normal.getLeadOffset());
    }

    @Test public void leadCanBeTurnedOff() {
        TurretAim.leadEnabled = false;
        TurretAim aim = new TurretAim(Alliance.RED);
        aim.update(X, Y, HEADING, 40, 0);
        assertFalse(aim.isLeading());
        assertEquals(aim.getRealDistance(), aim.getDistance(), 1e-9);
    }

    @Test public void shotTimeTableMatchesTheMeasuredRows() {
        ShooterData data = new ShooterData();
        assertEquals(0.50, data.getShotTimeVal(18), 1e-9);
        assertEquals(0.70, data.getShotTimeVal(50), 1e-9);
        assertEquals(0.85, data.getShotTimeVal(80), 1e-9);
        assertEquals(0.85, data.getShotTimeVal(200), 1e-6); // held flat past the last row
        assertEquals(data.INVALID, data.getShotTimeVal(0), 1e-9);
    }

    //---------------- Dynamic Aperture / Target X Shifting ----------------

    @Test public void targetXShiftsToMaximizeCrossSection() {
        TurretAim aiming = new TurretAim(Alliance.RED);
        double heading = Math.PI / 2.0;

        // 1. Center Test
        aiming.update(TurretAim.redCloseCellX, TurretAim.redCloseCellY - 30, heading);
        double centerTargetX = aiming.getTargetX();

        // 2. Left Side Test
        aiming.update(12, TurretAim.redCloseCellX - 30, heading);
        double leftTargetX = aiming.getTargetX();

        // 3. Right Side Test
        aiming.update(100, TurretAim.redCloseCellX - 30, heading);
        double rightTargetX = aiming.getTargetX();

        assertTrue("Target plane intersection should shift right when robot is left", leftTargetX > centerTargetX);
        assertTrue("Target plane intersection should shift right when robot is right", rightTargetX < centerTargetX);
    }

    @Test public void printTargetXGrid() {
        TurretAim aiming = new TurretAim(Alliance.RED);
        double heading = Math.PI / 2.0;

        System.out.println("\n=== TargetX Grid (Red Alliance) ===");
        System.out.printf("%-10s", "Y \\ X");

        for (int x = 0; x <= 144; x += 24) {
            System.out.printf("%-10d", x);
        }
        System.out.println();

        for (int y = 0; y <= 144; y += 24) {
            System.out.printf("%-10d", y);

            for (int x = 0; x <= 144; x += 24) {
                aiming.update(x, y, heading);
                System.out.printf("%-10.2f", aiming.getTargetX());
            }
            System.out.println();
        }
    }

    @Test
    public void distanceScaledSensitivityShiftsMoreWhenClose() {
        TurretAim aiming = new TurretAim(Alliance.RED);
        double heading = Math.PI / 2.0;

        // Red Close Cell base coordinates: X = 59.25, Y = 56.2
        // We put the robot at X = 48, creating a lateral offset of 11.25 inches.

        // 1. Test at 24 inches depth
        // Robot Y = 56.2 - 24 = 32.2
        aiming.update(48, 32.2, heading);
        double closeTargetX = aiming.getTargetX();

        // 2. Test at 96 inches depth
        // Robot Y = 56.2 - 96 = -39.8
        aiming.update(48, -39.8, heading);
        double farTargetX = aiming.getTargetX();

        System.out.println("Base Target: 59.25");
        System.out.println("TargetX at 24 inches deep: " + closeTargetX);
        System.out.println("TargetX at 96 inches deep: " + farTargetX);

        // --- The Logic Check ---
        // Prove that the shift is more aggressive when the robot is closer to the hive.
        // We avoid hardcoded expected values here because the exact math depends on
        // the physical turretOffsetForward and turretOffsetLeft defined in TurretAim.
        assertTrue("Target should shift more aggressively when close to the hive",
                closeTargetX > farTargetX);

        // Ensure it shifts in the correct direction (robot on left, target shifts right)
        assertTrue("Target should shift to the right when robot is on the left",
                closeTargetX > 59.25);
    }
}