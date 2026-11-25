package org.firstinspires.ftc.teamcode.autonomous.paths;

import com.pedropathing.api.PoseFactory;
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
            24.0 + RobotGeometry.robotHalfWidth,
            RobotGeometry.robotHalfLength + RobotGeometry.wall, Math.toRadians(270));

    // The wall gap is subtracted here: the rear wall is at y = 144, so moving away from it means smaller y
    public static final Pose RED_REAR_START = new Pose(
            FieldLandmarks.FIELD_CENTER_INCHES - RobotGeometry.robotHalfWidth,
            FieldLandmarks.FIELD_SIZE_INCHES - RobotGeometry.robotHalfLength - RobotGeometry.wall,
            Math.toRadians(90));

    // Switching alliances also switches which end of the field the pose is on.
    public static final Pose BLUE_AUDIENCE_START = rotateForOppositeAlliance(RED_REAR_START);
    public static final Pose BLUE_REAR_START = rotateForOppositeAlliance(RED_AUDIENCE_START);

    /**
     * Robot-center pose with the back against the audience wall and the left side
     * against the left wall, facing away from the audience (+Y).
     * Reference corner placement for diagnostics and pose resets.
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
     * Both axes include the {@link RobotGeometry#wall} gap, moved away from each wall, so red close is
     * {@link #AUDIENCE_LEFT_CORNER_START} plus the gap.
     */
    public static Pose allianceCorner(Alliance alliance, boolean far) {
        double x = alliance == Alliance.RED
                ? RobotGeometry.robotHalfWidth + RobotGeometry.wall
                : FieldLandmarks.FIELD_SIZE_INCHES - RobotGeometry.robotHalfWidth - RobotGeometry.wall;
        double y = far
                ? FieldLandmarks.FIELD_SIZE_INCHES - RobotGeometry.robotHalfLength - RobotGeometry.wall
                : RobotGeometry.robotHalfLength + RobotGeometry.wall;
        return new Pose(x, y, Math.toRadians(far ? -90 : 90));
    }

    /** Red Audience Two Tip waypoints and curve control points. */
    public static final class TwoTip {
        private static final PoseFactory DEGREES = PoseFactory.degrees();
        private static final Pose REAR_FLOWER_ANCHOR = FieldLandmarks.FLOWER_REAR_LEFT_WALL_ANCHOR;

        public static final Pose GARDEN = DEGREES.of(8.5, 8.0, 270);
        public static final Pose GARDEN_CONTROL = DEGREES.of(19.5, 21.0007, 0);
        public static final Pose CROSS_FIELD_CONTROL_1 = DEGREES.of(8.5, 24.5, 0);
        public static final Pose CROSS_FIELD_CONTROL_2 = DEGREES.of(40.25, 56.5, 0);
        public static final Pose CROSS_FIELD_CONTROL_3 = DEGREES.of(20.5, 103, 0);

        // Distances from the rear wall to the robot center along the Flower centerline.
        public static final Pose REAR_FLOWER_LINEUP = DEGREES.of(
                REAR_FLOWER_ANCHOR.x(), REAR_FLOWER_ANCHOR.y() - 25.0, -159.3045);
        public static final Pose REAR_FLOWER_FACING_WALL = REAR_FLOWER_LINEUP.withHeading(Math.PI / 2);
        public static final Pose REAR_FLOWER_INTAKE = DEGREES.of(
                REAR_FLOWER_ANCHOR.x(), REAR_FLOWER_ANCHOR.y() - 16.125, 90);
        public static final Pose PARK = DEGREES.of(16, 114.5, 90);

        private TwoTip() {}
    }

    private FieldPoses() {}
}
