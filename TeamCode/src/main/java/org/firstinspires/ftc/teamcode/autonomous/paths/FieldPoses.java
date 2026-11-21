package org.firstinspires.ftc.teamcode.autonomous.paths;

import com.pedropathing.math.Pose;
import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
import org.firstinspires.ftc.teamcode.config.RobotGeometry;

/**
 * Shared robot-center starting poses and path waypoints. Positions use inches
 * in the Pedro coordinate frame described in {@link FieldLandmarks}.
 * Headings are radians, counterclockwise from +X.
 */
public final class FieldPoses {
    public static final Pose RED_AUDIENCE_START = new Pose(
            24.0 + RobotGeometry.robotHalfWidth, RobotGeometry.robotHalfLength, Math.toRadians(270));

    public static final Pose RED_REAR_START = new Pose(
            FieldLandmarks.FIELD_CENTER_INCHES - RobotGeometry.robotHalfWidth,
            FieldLandmarks.FIELD_SIZE_INCHES - RobotGeometry.robotHalfLength, Math.toRadians(90));

    // Switching alliances also switches which end of the field the pose is on.
    public static final Pose BLUE_AUDIENCE_START = rotateForOppositeAlliance(RED_REAR_START);
    public static final Pose BLUE_REAR_START = rotateForOppositeAlliance(RED_AUDIENCE_START);

    /**
     * Robot-center pose with the back against the audience wall and the left side
     * against the left wall, facing away from the audience (+Y).
     * Shared starting placement for autonomous and independently started TeleOp.
     */
    public static final Pose AUDIENCE_LEFT_CORNER_START = new Pose(
            RobotGeometry.robotHalfWidth, RobotGeometry.robotHalfLength, Math.toRadians(90));

    /** Rotates a robot pose 180 degrees about field center to switch alliances. */
    public static Pose rotateForOppositeAlliance(Pose pose) {
        return new Pose(FieldLandmarks.FIELD_SIZE_INCHES - pose.x(),
                FieldLandmarks.FIELD_SIZE_INCHES - pose.y(), pose.heading() + Math.PI);
    }

    /**
     * Robot-center pose in a corner on the alliance's side wall (red = left, blue = right, seen from the
     * audience: the same sides as each alliance's hive). Close = the audience-wall corner, back against the
     * audience wall, facing +Y. Far = the rear-wall corner, back against the rear wall, facing -Y.
     * Either way the robot faces away from the wall behind it. Used for TeleOp pose resets.
     * Red close is the same pose as {@link #AUDIENCE_LEFT_CORNER_START}.
     */
    public static Pose allianceCorner(Alliance alliance, boolean far) {
        double x = alliance == Alliance.RED
                ? RobotGeometry.robotHalfWidth
                : FieldLandmarks.FIELD_SIZE_INCHES - RobotGeometry.robotHalfWidth;
        double y = far
                ? FieldLandmarks.FIELD_SIZE_INCHES - RobotGeometry.robotHalfLength
                : RobotGeometry.robotHalfLength;
        return new Pose(x, y, Math.toRadians(far ? -90 : 90));
    }

    private FieldPoses() {}
}
