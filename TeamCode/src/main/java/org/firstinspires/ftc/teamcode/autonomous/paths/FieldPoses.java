package org.firstinspires.ftc.teamcode.autonomous.paths;

import com.pedropathing.math.Pose;
import org.firstinspires.ftc.teamcode.config.RobotGeometry;

/**
 * Shared named locations, such as future starting, scoring, and pickup poses.
 * Path classes use these coordinates to build routes; this class does not build paths.
 * TEST_START is a local origin at the robot's placement, not a measured field location.
 */
public final class FieldPoses {
    public static final Pose TEST_START = Pose.zero();

    /**
     * Robot-center pose with the back against the audience wall and the left side
     * against the left wall, facing away from the audience (+Y).
     * Shared starting placement for autonomous and independently started TeleOp.
     */
    public static final Pose AUDIENCE_LEFT_CORNER_START = new Pose(
            RobotGeometry.robotHalfWidth, RobotGeometry.robotHalfLength, Math.toRadians(90));

    private FieldPoses() {}
}
