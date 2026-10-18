package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoContext;
import org.firstinspires.ftc.teamcode.opmodes.autonomous.SelectableAutoOpMode;

/**
 * Driver Station entry point "Auto Path Test": select from the same catalog as Auto Select.
 * Runs real driving paths while mechanism actions created through AutoContext are skipped.
 * No mechanism Robot is initialized. This verifies movement and route order; it does not
 * verify mechanism readiness, sensor-driven decisions, or full-auto timing.
 */
@Autonomous(name = "Auto Path Test", group = "Tests")
public final class AutoPathTest extends SelectableAutoOpMode {
    @Override protected AutoContext.Mode executionMode() { return AutoContext.Mode.PATH_TEST; }
}
