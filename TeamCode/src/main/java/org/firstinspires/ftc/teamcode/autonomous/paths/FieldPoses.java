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

    public static final Pose RED_REAR_START = new Pose(
            FieldLandmarks.FIELD_CENTER_INCHES - RobotGeometry.robotHalfWidth,
            FieldLandmarks.FIELD_SIZE_INCHES - RobotGeometry.robotHalfLength - RobotGeometry.wall,
            Math.toRadians(90));

    public static final Pose BLUE_AUDIENCE_START = rotateForOppositeAlliance(RED_REAR_START);
    public static final Pose BLUE_REAR_START = rotateForOppositeAlliance(RED_AUDIENCE_START);

    public static final Pose AUDIENCE_LEFT_CORNER_START = new Pose(
            RobotGeometry.robotHalfWidth, RobotGeometry.robotHalfLength, Math.toRadians(90));

    public static Pose rotateForOppositeAlliance(Pose pose) {
        return new Pose(FieldLandmarks.FIELD_SIZE_INCHES - pose.x(),
                FieldLandmarks.FIELD_SIZE_INCHES - pose.y(), pose.heading() + Math.PI);
    }

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

        // --- Close Points ---
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

        // --- Far Points ---
        public static final Pose FAR_PRELOAD_TO_REAR_FLOWER = DEGREES.of(46.4991, 131.2623, 90);
        public static final Pose FAR_PRELOAD_TO_REAR_FLOWER_CONTROL = DEGREES.of(46.5753, 102.6211, 0);

        public static final Pose FAR_SEGMENT_1_START = DEGREES.of(46.4991, 131.2623, 270);
        public static final Pose FAR_SEGMENT_1_END = DEGREES.of(46.4991, 131.2623, 90);

        public static final Pose FAR_SEGMENT_2_START = DEGREES.of(46.4991, 131.2623, 90);
        public static final Pose FAR_SEGMENT_2_END = DEGREES.of(46.4991, 131.2623, 90);

        public static final Pose FAR_PARK = DEGREES.of(14.1444, 118.2276, 90);

        private TwoTip() {}
    }

    /** Three Tip waypoints, styled to match TwoTip conventions. */
    /** Three Tip waypoints, styled to match TwoTip conventions. */
    public static final class ThreeTip {
        private static final PoseFactory DEGREES = PoseFactory.degrees();
        private static final Pose AUDIENCE_FLOWER_ANCHOR = FieldLandmarks.FLOWER_AUDIENCE_LEFT_WALL_ANCHOR;
        private static final Pose PARK_FLOWER_ANCHOR = FieldLandmarks.FLOWER_PARK_LEFT_WALL_ANCHOR;
        private static final Pose REAR_FLOWER_ANCHOR = FieldLandmarks.FLOWER_REAR_LEFT_WALL_ANCHOR;


        // ==========================================
        // ---------- CLOSE POSES (Audience) --------
        // ==========================================
        public static final Pose PARK_FLOWER_LINEUP = DEGREES.of(
                PARK_FLOWER_ANCHOR.x() + 16.125, PARK_FLOWER_ANCHOR.y() - 25.0, 180);
        public static final Pose PARK_FLOWER_FACING_WALL = PARK_FLOWER_LINEUP.withHeading(Math.PI);
        public static final Pose REAR_FLOWER_LINEUP = DEGREES.of(
                REAR_FLOWER_ANCHOR.x(), REAR_FLOWER_ANCHOR.y() - 25.0, -159.3045);
        public static final Pose REAR_FLOWER_FACING_WALL = REAR_FLOWER_LINEUP.withHeading(Math.PI / 2);

        public static final Pose GARDEN = DEGREES.of(8.5, 8.0, 270);
        public static final Pose GARDEN_CONTROL = DEGREES.of(19.5, 21.0, 0);

        public static final Pose GARDEN_TO_SIDE_FLOWER_CONTROL = DEGREES.of(45.0348, 49.113, 0);
        public static final Pose SIDE_FLOWER = DEGREES.of(10.3843, 46.4092, 180);

        public static final Pose FLOWER_SEGMENT_1_START = DEGREES.of(10.3843, 46.4092, 270);
        public static final Pose FLOWER_SEGMENT_1_END = DEGREES.of(10.3843, 46.4092, 270);
        public static final Pose FLOWER_SEGMENT_2_START = DEGREES.of(10.3843, 46.4092, -90);
        public static final Pose FLOWER_SEGMENT_2_END = DEGREES.of(10.3843, 46.4092, 180);
        public static final Pose FLOWER_SEGMENT_3_START = DEGREES.of(10.3843, 46.4092, 180);
        public static final Pose FLOWER_SEGMENT_3_END = DEGREES.of(10.3843, 46.4092, 180);

        public static final Pose SIDE_FLOWER_TO_SHOOT = DEGREES.of(39.9462, 17.3904, 180);
        public static final Pose PARK = DEGREES.of(11.0556, 89.3208, 180);

        // ==========================================
        // -------------- FAR POSES (Rear) ----------
        // ==========================================

        public static final Pose FAR_PRELOAD_TO_REAR_FLOWER = DEGREES.of(46.4991, 131.2623, 90);
        public static final Pose FAR_PRELOAD_TO_REAR_FLOWER_CONTROL = DEGREES.of(46.5753, 102.6211, 0);
        public static final Pose FAR_PRELOAD_SEG1_START = DEGREES.of(46.4991, 131.2623, 90);
        public static final Pose FAR_PRELOAD_SEG1_END = DEGREES.of(46.4991, 131.2623, 90);
        public static final Pose FAR_PRELOAD_SEG2_START = DEGREES.of(46.4991, 131.2623, 90);
        public static final Pose FAR_PRELOAD_SEG2_END = DEGREES.of(46.4991, 131.2623, 90);

        // Note: Using visualizer's exact -90 to 270 bounds to ensure Pedro heading interpolates correctly
        public static final Pose FAR_REAR_FLOWER_TO_GARDEN = DEGREES.of(8.5, 8.0, -90);
        public static final Pose FAR_GARDEN_SEG1_START = DEGREES.of(8.5, 8.0, 90);
        public static final Pose FAR_GARDEN_SEG1_END = DEGREES.of(8.5, 8.0, 90);
        public static final Pose FAR_GARDEN_SEG2_START = DEGREES.of(8.5, 8.0, 90);
        public static final Pose FAR_GARDEN_SEG2_END = DEGREES.of(8.5, 8.0, 270);
        public static final Pose FAR_GARDEN_SEG3_START = DEGREES.of(8.5, 8.0, -90);
        public static final Pose FAR_GARDEN_SEG3_END = DEGREES.of(8.5, 8.0, 270);

        public static final Pose FAR_GARDEN_TO_SIDE_FLOWER_CONTROL = DEGREES.of(65.1152, 47.7503, 0);
        public static final Pose FAR_SIDE_FLOWER = DEGREES.of(12.2113, 47.1188, 180);

        public static final Pose FAR_SIDE_FLOWER_SEG1_START = DEGREES.of(12.2113, 47.1188, 270);
        public static final Pose FAR_SIDE_FLOWER_SEG1_END = DEGREES.of(12.2113, 47.1188, 270);
        public static final Pose FAR_SIDE_FLOWER_SEG2_START = DEGREES.of(12.2113, 47.1188, 270);
        public static final Pose FAR_SIDE_FLOWER_SEG2_END = DEGREES.of(12.2113, 47.1188, 180);
        public static final Pose FAR_SIDE_FLOWER_SEG3_START = DEGREES.of(12.2113, 47.1188, 180);
        public static final Pose FAR_SIDE_FLOWER_SEG3_END = DEGREES.of(12.2113, 47.1188, 180);

        public static final Pose FAR_SIDE_FLOWER_TO_SHOOT = DEGREES.of(37.8136, 23.7782, 180);
        public static final Pose FAR_PARK = DEGREES.of(11.4403, 89.2764, 180);


        public static final Pose AUDIENCE_FLOWER_LINEUP = DEGREES.of(
                AUDIENCE_FLOWER_ANCHOR.x(), AUDIENCE_FLOWER_ANCHOR.y() + 25.0, 180);
        public static final Pose AUDIENCE_FLOWER_FACING_WALL = AUDIENCE_FLOWER_LINEUP.withHeading(Math.toRadians(270));
        public static final Pose AUDIENCE_FLOWER_INTAKE = DEGREES.of(
                AUDIENCE_FLOWER_ANCHOR.x(), AUDIENCE_FLOWER_ANCHOR.y() + 16.125, 270);

        private ThreeTip() {}
    }

    private FieldPoses() {}
}