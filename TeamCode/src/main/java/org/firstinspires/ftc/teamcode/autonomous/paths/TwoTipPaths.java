package org.firstinspires.ftc.teamcode.autonomous.paths;

import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import org.firstinspires.ftc.teamcode.autonomous.paths.FieldPoses.TwoTip;
import org.firstinspires.ftc.teamcode.pedro.Constants;

/** Driving legs for the Red Audience Two Tip route. */
public final class TwoTipPaths {
    public final Path startToGarden;
    public final Path gardenToRearFlowerLineup;
    public final Path rearFlowerApproach;
    public final Path flowerToPark;

    public TwoTipPaths(Pose startPose) {
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
    }
}
