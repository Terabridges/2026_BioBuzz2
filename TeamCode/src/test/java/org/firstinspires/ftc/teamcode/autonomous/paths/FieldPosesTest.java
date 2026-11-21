package org.firstinspires.ftc.teamcode.autonomous.paths;

import static org.junit.Assert.assertEquals;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
import org.firstinspires.ftc.teamcode.config.RobotGeometry;
import org.junit.Test;

/** Shared starting poses and the field transform used to switch alliances. */
public class FieldPosesTest {
    private static final double HALF_W = RobotGeometry.robotHalfWidth;   // 7.58 in.
    private static final double HALF_L = RobotGeometry.robotHalfLength;  // 8.19 in.

    private static void assertPose(Pose pose, double x, double y, double headingDeg) {
        assertEquals(x, pose.x(), 1e-9);
        assertEquals(y, pose.y(), 1e-9);
        // Pedro stores headings as 0–360, so -90 comes back as 270 (the same direction). Compare wrapped
        double diff = Math.toDegrees(pose.heading()) - headingDeg;
        assertEquals(0, ((diff % 360) + 540) % 360 - 180, 1e-9);
    }

    @Test public void redCloseIsTheAudienceLeftCornerStart() {
        Pose start = FieldPoses.AUDIENCE_LEFT_CORNER_START;
        assertPose(FieldPoses.allianceCorner(Alliance.RED, false), start.x(), start.y(), Math.toDegrees(start.heading()));
    }

    @Test public void redFarIsTheRearLeftCornerFacingTheAudience() {
        assertPose(FieldPoses.allianceCorner(Alliance.RED, true), HALF_W, 144 - HALF_L, -90);
    }

    @Test public void blueCornersAreOnTheRightWall() {
        assertPose(FieldPoses.allianceCorner(Alliance.BLUE, false), 144 - HALF_W, HALF_L, 90);
        assertPose(FieldPoses.allianceCorner(Alliance.BLUE, true), 144 - HALF_W, 144 - HALF_L, -90);
    }

    @Test public void oppositeAllianceRotatesPositionAndHeadingAboutFieldCenter() {
        Pose redPose = new Pose(30, 40, Math.toRadians(35));
        Pose bluePose = FieldPoses.rotateForOppositeAlliance(redPose);
        assertPose(bluePose, 114, 104, 215);
        assertPose(FieldPoses.rotateForOppositeAlliance(bluePose), 30, 40, 35);
    }

    @Test public void autonomousStartsMatchTheSelectedEndOfTheField() {
        double wall = 1.0;
        assertPose(FieldPoses.RED_AUDIENCE_START, 24 + HALF_W, HALF_L + wall, 270);
        assertPose(FieldPoses.RED_REAR_START, 72 - HALF_W, 144 - HALF_L + wall, 90);
        assertPose(FieldPoses.BLUE_AUDIENCE_START, 72 + HALF_W, HALF_L - wall, 270);
        assertPose(FieldPoses.BLUE_REAR_START, 120 - HALF_W, 144 - HALF_L - wall, 90);
    }
}
