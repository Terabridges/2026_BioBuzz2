package org.firstinspires.ftc.teamcode.utility.shooterUtility;

import static org.junit.Assert.*;

import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
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
}
