package org.firstinspires.ftc.teamcode.autonomous.paths;

import com.pedropathing.math.Pose;

/**
 * BIOBUZZ field dimensions and landmark coordinates in inches.
 * From the audience view, the origin is the near-left inside corner, +X points
 * right, and +Y points toward the rear of the field.
 *
 * The 144-inch planning frame matches PoseLogging and Pedro's coordinate conversion.
 * Positions are derived from nominal field dimensions, tile locations, and CAD.
 * Landmark headings are zero placeholders. Use robot-center placements from
 * {@link FieldPoses} when building paths.
 *
 * Derivations and official sources are in TeamCode/doc/field-poses.md.
 */
public final class FieldLandmarks {
    public static final double FIELD_SIZE_INCHES = 144.0;
    public static final double FIELD_CENTER_INCHES = 72.0;

    public static final double LOADING_ZONE_WIDTH_INCHES = 23.0;
    public static final double LOADING_ZONE_DEPTH_INCHES = 11.0;
    public static final double GARDEN_WIDTH_INCHES = 23.0;
    public static final double GARDEN_DEPTH_INCHES = 2.0;
    public static final double HIVE_FRAME_WIDTH_INCHES = 49.46;
    public static final double HIVE_FRAME_DEPTH_INCHES = 38.95;

    /** Center of the fixed HIVE frame. */
    public static final Pose HIVE_FRAME_CENTER =
            new Pose(FIELD_CENTER_INCHES, FIELD_CENTER_INCHES);

    /** Nominal height of the raised opening above the tiles. */
    public static final double HIVE_CELL_UP_AIM_HEIGHT_INCHES = 59.5;

    // Opening centers from official CAD v26-27.2, assuming the named CELL is fully raised.
    // Only one CELL per alliance is raised at a time.
    public static final Pose RED_HIVE_AUDIENCE_CELL_UP_AIM =
            new Pose(59.25, 56.2);
    public static final Pose RED_HIVE_REAR_CELL_UP_AIM =
            new Pose(59.25, 87.8);
    public static final Pose BLUE_HIVE_AUDIENCE_CELL_UP_AIM =
            new Pose(84.75, 56.2);
    public static final Pose BLUE_HIVE_REAR_CELL_UP_AIM =
            new Pose(84.75, 87.8);

    // Gardens at A1 (red) and F6 (blue), measured to the centers of the taped strips.
    public static final Pose RED_GARDEN_CENTER =
            new Pose(11.5, 1.0);
    public static final Pose BLUE_GARDEN_CENTER =
            new Pose(132.5, 143.0);

    // Loading Zones at A5 (red) and F2 (blue). Width runs along the side wall.
    public static final Pose RED_LOADING_ZONE_CENTER =
            new Pose(5.5, 108.0);
    public static final Pose BLUE_LOADING_ZONE_CENTER =
            new Pose(138.5, 36.0);

    // Flower wall attachment points at the B/C and D/E seams.
    public static final Pose FLOWER_AUDIENCE_LEFT_WALL_ANCHOR =
            new Pose(48.0, 0.0);
    public static final Pose FLOWER_AUDIENCE_RIGHT_WALL_ANCHOR =
            new Pose(96.0, 0.0);
    public static final Pose FLOWER_REAR_LEFT_WALL_ANCHOR =
            new Pose(48.0, FIELD_SIZE_INCHES);
    public static final Pose FLOWER_REAR_RIGHT_WALL_ANCHOR =
            new Pose(96.0, FIELD_SIZE_INCHES);

    private FieldLandmarks() {}
}
