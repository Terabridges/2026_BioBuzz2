package org.firstinspires.ftc.teamcode.autonomous.paths;

import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

/** Example paths built once when a routine is created. */
public final class AutoPaths {
    public final Path startToScore;
    public final Path scoreToPark;

    public AutoPaths(Pose startPose, Pose scorePose, Pose parkControlPoint, Pose parkPose) {
        startToScore = Paths.line(startPose, scorePose)
                .linear(startPose, scorePose);

        // The robot follows the curve shaped by the control point between the endpoints.
        scoreToPark = Paths.curve(scorePose, parkControlPoint, parkPose)
                .linear(scorePose, parkPose);
    }
}
