package org.firstinspires.ftc.teamcode.autonomous.paths;

import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.interpolator.Interpolator;

import org.firstinspires.ftc.teamcode.autonomous.paths.FieldPoses.ThreeTip;

/** Driving legs for the Three Tip route (contains both Close and Far variations). */
public final class ThreeTipPaths {

    // --- Close Paths ---
    public final Path closeStartToGarden;
    public final Path closeGardenToSideFlower;
    public final Path closeSideFlowerToShoot;
    public final Path closeShootToPark;

    // --- Far Paths ---
    public final Path farStartToRearFlower;
    public final Path farRearFlowerToGarden;
    public final Path farGardenToSideFlower;
    public final Path farSideFlowerToShoot;
    public final Path farShootToPark;

    public ThreeTipPaths(Pose startPose) {
        // ---------- CLOSE ROUTE PATHS -------------
        closeStartToGarden = Paths.curve(startPose, ThreeTip.GARDEN_CONTROL, ThreeTip.GARDEN)
                .linear(startPose, ThreeTip.GARDEN);

        closeGardenToSideFlower = Paths.curve(ThreeTip.GARDEN, ThreeTip.GARDEN_TO_SIDE_FLOWER_CONTROL, ThreeTip.SIDE_FLOWER)
                .heading(Interpolator.piecewise()
                        .until(0.6385, Interpolator.linear(ThreeTip.FLOWER_SEGMENT_1_START, ThreeTip.FLOWER_SEGMENT_1_END))
                        .until(0.8183, Interpolator.linear(ThreeTip.FLOWER_SEGMENT_2_START, ThreeTip.FLOWER_SEGMENT_2_END))
                        .until(1.0, Interpolator.linear(ThreeTip.FLOWER_SEGMENT_3_START, ThreeTip.FLOWER_SEGMENT_3_END)));

        closeSideFlowerToShoot = Paths.line(ThreeTip.SIDE_FLOWER, ThreeTip.SIDE_FLOWER_TO_SHOOT)
                .constant(ThreeTip.SIDE_FLOWER_TO_SHOOT);

        closeShootToPark = Paths.line(ThreeTip.SIDE_FLOWER_TO_SHOOT, ThreeTip.PARK)
                .constant(ThreeTip.PARK);

        // ----------- FAR ROUTE PATHS --------------
        farStartToRearFlower = Paths.curve(startPose, ThreeTip.FAR_PRELOAD_TO_REAR_FLOWER_CONTROL, ThreeTip.FAR_PRELOAD_TO_REAR_FLOWER)
                .heading(Interpolator.piecewise()
                        .until(0.7211, Interpolator.linear(ThreeTip.FAR_PRELOAD_SEG1_START, ThreeTip.FAR_PRELOAD_SEG1_END))
                        .until(1.0, Interpolator.linear(ThreeTip.FAR_PRELOAD_SEG2_START, ThreeTip.FAR_PRELOAD_SEG2_END)));

        farRearFlowerToGarden = Paths.line(ThreeTip.FAR_PRELOAD_TO_REAR_FLOWER, ThreeTip.FAR_REAR_FLOWER_TO_GARDEN)
                .heading(Interpolator.piecewise()
                        .until(0.2569, Interpolator.linear(ThreeTip.FAR_GARDEN_SEG1_START, ThreeTip.FAR_GARDEN_SEG1_END))
                        .until(0.6789, Interpolator.linear(ThreeTip.FAR_GARDEN_SEG2_START, ThreeTip.FAR_GARDEN_SEG2_END))
                        .until(1.0, Interpolator.linear(ThreeTip.FAR_GARDEN_SEG3_START, ThreeTip.FAR_GARDEN_SEG3_END)));

        farGardenToSideFlower = Paths.curve(ThreeTip.FAR_REAR_FLOWER_TO_GARDEN, ThreeTip.FAR_GARDEN_TO_SIDE_FLOWER_CONTROL, ThreeTip.FAR_SIDE_FLOWER)
                .heading(Interpolator.piecewise()
                        .until(0.2514, Interpolator.linear(ThreeTip.FAR_SIDE_FLOWER_SEG1_START, ThreeTip.FAR_SIDE_FLOWER_SEG1_END))
                        .until(0.6257, Interpolator.linear(ThreeTip.FAR_SIDE_FLOWER_SEG2_START, ThreeTip.FAR_SIDE_FLOWER_SEG2_END))
                        .until(1.0, Interpolator.linear(ThreeTip.FAR_SIDE_FLOWER_SEG3_START, ThreeTip.FAR_SIDE_FLOWER_SEG3_END)));

        farSideFlowerToShoot = Paths.line(ThreeTip.FAR_SIDE_FLOWER, ThreeTip.FAR_SIDE_FLOWER_TO_SHOOT)
                .constant(ThreeTip.FAR_SIDE_FLOWER_TO_SHOOT);

        farShootToPark = Paths.line(ThreeTip.FAR_SIDE_FLOWER_TO_SHOOT, ThreeTip.FAR_PARK)
                .constant(ThreeTip.FAR_PARK);
    }
}