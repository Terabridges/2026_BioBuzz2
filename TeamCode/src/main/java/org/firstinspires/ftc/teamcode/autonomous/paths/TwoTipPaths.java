package org.firstinspires.ftc.teamcode.autonomous.paths;

import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.interpolator.Interpolator;

import org.firstinspires.ftc.teamcode.autonomous.paths.FieldPoses.TwoTip;
import org.firstinspires.ftc.teamcode.pedro.Constants;

/** Driving legs for the Red Audience Two Tip route. */
public final class TwoTipPaths {

    // --- Close Paths ---
    public final Path startToGarden;
    public final Path gardenToRearFlowerLineup;
    public final Path rearFlowerApproach;
    public final Path flowerToPark;

    // --- Far Paths ---
    public final Path farPreloadToRearFlower;
    public final Path farRearFlowerToPark;

    public TwoTipPaths(Pose startPose) {
        // Close Routine
        startToGarden = Paths.curve(startPose, TwoTip.GARDEN_CONTROL, TwoTip.GARDEN)
                .linear(startPose, TwoTip.GARDEN);

        gardenToRearFlowerLineup = Paths.curve(TwoTip.GARDEN, TwoTip.CROSS_FIELD_CONTROL_1,
                        TwoTip.CROSS_FIELD_CONTROL_2, TwoTip.CROSS_FIELD_CONTROL_3, TwoTip.REAR_FLOWER_LINEUP)
                .linear(TwoTip.GARDEN, TwoTip.REAR_FLOWER_LINEUP);

        rearFlowerApproach = Paths.line(TwoTip.REAR_FLOWER_FACING_WALL, TwoTip.REAR_FLOWER_INTAKE)
                .constant(TwoTip.REAR_FLOWER_INTAKE)
                .with(Constants.foresightConfig.maxPathSpeed.at(0.5));

        flowerToPark = Paths.line(TwoTip.REAR_FLOWER_INTAKE, TwoTip.PARK)
                .constant(TwoTip.PARK);

        // Far Routine
        farPreloadToRearFlower = Paths.curve(startPose, TwoTip.FAR_PRELOAD_TO_REAR_FLOWER_CONTROL, TwoTip.FAR_PRELOAD_TO_REAR_FLOWER)
                .heading(Interpolator.piecewise()
                        .until(0.7211, Interpolator.linear(TwoTip.FAR_SEGMENT_1_START, TwoTip.FAR_SEGMENT_1_END))
                        .until(1.0, Interpolator.linear(TwoTip.FAR_SEGMENT_2_START, TwoTip.FAR_SEGMENT_2_END)));

        farRearFlowerToPark = Paths.line(TwoTip.FAR_PRELOAD_TO_REAR_FLOWER, TwoTip.FAR_PARK)
                .constant(TwoTip.FAR_PARK);
    }
}