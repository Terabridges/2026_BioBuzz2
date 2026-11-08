package org.firstinspires.ftc.teamcode.autonomous.paths;

import com.pedropathing.math.Pose;

/**
 * BIOBUZZ field landmarks in Pedro coordinates (inches).
 * Viewed from the audience: origin is the near-left inside corner, +X points right,
 * and +Y points toward the rear/scoring side. Headings are radians, CCW from +X.
 *
 * Coordinates are calculated from official nominal dimensions, tile locations, and CAD;
 * FIRST does not publish these as exact Pedro coordinates. They are not surveyed.
 * These are field landmarks, NOT robot-center drive targets.
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
