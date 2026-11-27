package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoConfig;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoContext;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoRoutine;
import org.firstinspires.ftc.teamcode.autonomous.framework.StartSide;
import org.firstinspires.ftc.teamcode.autonomous.paths.FieldPoses;
import org.firstinspires.ftc.teamcode.autonomous.routines.ShootPreloadRoutine;
import org.firstinspires.ftc.teamcode.opmodes.autonomous.BaseAutoOpMode;

/** Exercises the same autonomous actions from the existing red corner test placement. */
@Autonomous(name = "Auto Preload Test", group = "Tests")
public final class AutoPreloadTest extends BaseAutoOpMode {
    private static final AutoConfig CONFIG = new AutoConfig("Stationary preload", Alliance.RED,
            StartSide.AUDIENCE, FieldPoses.AUDIENCE_LEFT_CORNER_START, false);

    @Override protected AutoConfig selectedConfig() { return CONFIG; }

    @Override protected AutoRoutine createRoutine(AutoContext context) {
        // The routine now automatically pulls the TurretAim.Cell from the context's StartSide
        return new ShootPreloadRoutine(context);
    }

    @Override protected void addSelectionTelemetry() {
        super.addSelectionTelemetry();
        telemetry.addLine("Place the robot in the audience-left corner, facing away from the audience.");
        telemetry.addLine("Load a preload and use the red close Cell. This test shoots from a stop.");
    }
}