package org.firstinspires.ftc.teamcode.autonomous.paths;

import com.pedropathing.math.Pose;

/**
 * Shared named locations, such as future starting, scoring, and pickup poses.
 * Path classes use these coordinates to build routes; this class does not build paths.
 * TEST_START is a local origin at the robot's placement, not a measured field location.
 * Add season field coordinates once measured.
 */
public final class FieldPoses {
    public static final Pose TEST_START = Pose.zero();

    private FieldPoses() {}
}
