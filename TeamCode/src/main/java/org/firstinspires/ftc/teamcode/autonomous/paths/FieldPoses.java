package org.firstinspires.ftc.teamcode.autonomous.paths;

import com.pedropathing.math.Pose;
import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
import org.firstinspires.ftc.teamcode.config.RobotGeometry;

/**
 * BIOBUZZ field landmarks in Pedro coordinates (inches).
 * Viewed from the audience: origin is the near-left inside corner, +X points right,
 * and +Y points toward the rear/scoring side. Headings are radians, CCW from +X.
 *
 * Coordinates are calculated from official nominal dimensions, tile locations, and CAD;
 * FIRST does not publish these as exact Pedro coordinates. They are not surveyed.
 * Landmarks are field features, NOT robot-center drive targets; the *_START poses
 * are the exception and are robot-center starting placements.
 * Landmark headings are zero placeholders. Paths must choose their own headings
 * and account for robot size, mechanism reach, obstacles, and measured field variation.
 * The 144-inch planning frame matches PoseLogging and Pedro's documented conversion;
 * do not mix it with a 141.5-inch wall-origin frame or scale physical inches.
 *
 * Sources: FIRST BIOBUZZ ARENA TU06, figures 9-2 through 9-8; Event Field Setup
 * Guide V1.0, sections 8-10. Derivations and source links: TeamCode/doc/field-poses.md.
 * This class stores locations only; path classes build routes.
 */
public final class FieldPoses {
    public static final double FIELD_SIZE_INCHES = 144.0;
    public static final double FIELD_CENTER_INCHES = 72.0;

    public static final double LOADING_ZONE_WIDTH_INCHES = 23.0;
    public static final double LOADING_ZONE_DEPTH_INCHES = 11.0;
    public static final double GARDEN_WIDTH_INCHES = 23.0;
    public static final double GARDEN_DEPTH_INCHES = 2.0;
    public static final double HIVE_FRAME_WIDTH_INCHES = 49.46;
    public static final double HIVE_FRAME_DEPTH_INCHES = 38.95;

    /** Local origin for Do Nothing / tuning, not a competition starting pose. */
    public static final Pose TEST_START = Pose.zero();

    /**
     * Robot-center pose with the back against the audience wall and the left side
     * against the left wall, facing away from the audience (+Y).
     * Shared starting placement for autonomous and independently started TeleOp.
     */
    public static final Pose AUDIENCE_LEFT_CORNER_START = new Pose(
            RobotGeometry.robotHalfWidth, RobotGeometry.robotHalfLength, Math.toRadians(90));

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
                : FIELD_SIZE_INCHES - RobotGeometry.robotHalfWidth;
        double y = far
                ? FIELD_SIZE_INCHES - RobotGeometry.robotHalfLength
                : RobotGeometry.robotHalfLength;
        return new Pose(x, y, Math.toRadians(far ? -90 : 90));
    }

    /** Center of the fixed frame, not a CELL opening or a traversable waypoint. */
    public static final Pose HIVE_FRAME_CENTER =
            new Pose(72.0, 72.0);

    /** Nominal raised opening aim height above the tiles; Pose stores X/Y only. */
    public static final double HIVE_CELL_UP_AIM_HEIGHT_INCHES = 59.5;

    // Centers of the opening bounds, each assuming THAT CELL is fully UP.
    // From official CAD v26-27.2, centered in our nominal 144-inch Pedro frame.
    // Only one CELL per alliance is up at a time. Zero headings are placeholders.
    public static final Pose RED_HIVE_AUDIENCE_CELL_UP_AIM =
            new Pose(59.25, 56.2);
    public static final Pose RED_HIVE_REAR_CELL_UP_AIM =
            new Pose(59.25, 87.8);
    public static final Pose BLUE_HIVE_AUDIENCE_CELL_UP_AIM =
            new Pose(84.75, 56.2);
    public static final Pose BLUE_HIVE_REAR_CELL_UP_AIM =
            new Pose(84.75, 87.8);

    // Gardens: A1 (red) and F6 (blue). Centers of the narrow taped strips.
    public static final Pose RED_GARDEN_CENTER =
            new Pose(11.5, 1.0);
    public static final Pose BLUE_GARDEN_CENTER =
            new Pose(132.5, 143.0);

    // Loading zones: A5 (red) and F2 (blue). Width runs along the side wall.
    public static final Pose RED_LOADING_ZONE_CENTER =
            new Pose(5.5, 108.0);
    public static final Pose BLUE_LOADING_ZONE_CENTER =
            new Pose(138.5, 36.0);

    // Flower mounts are on the audience/rear walls at the B/C and D/E seams.
    // These are WALL attachment references, not the inset ring/opening centers.
    public static final Pose FLOWER_AUDIENCE_LEFT_WALL_ANCHOR =
            new Pose(48.0, 0.0);
    public static final Pose FLOWER_AUDIENCE_RIGHT_WALL_ANCHOR =
            new Pose(96.0, 0.0);
    public static final Pose FLOWER_REAR_LEFT_WALL_ANCHOR =
            new Pose(48.0, 144.0);
    public static final Pose FLOWER_REAR_RIGHT_WALL_ANCHOR =
            new Pose(96.0, 144.0);

    private FieldPoses() {}
}
