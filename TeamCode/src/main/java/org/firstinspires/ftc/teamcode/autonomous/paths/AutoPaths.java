package org.firstinspires.ftc.teamcode.autonomous.paths;

import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

/** Builds autonomous paths between robot-center poses. */
public final class AutoPaths {
    /**
     * Builds a straight path between two robot poses with linear heading interpolation.
     * A routine can build it once when it is created, then follow it when ready to drive.
     */
    public static Path buildPaths(Pose startPose, Pose endPose) {
        return Paths.line(startPose, endPose).linear(startPose, endPose);
    }

    private AutoPaths() {}
}
