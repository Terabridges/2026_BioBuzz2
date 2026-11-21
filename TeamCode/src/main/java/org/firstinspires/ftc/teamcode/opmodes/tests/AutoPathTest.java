package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoContext;
import org.firstinspires.ftc.teamcode.opmodes.autonomous.SelectableAutoOpMode;

/** Runs the selected autonomous routine with path test mode enabled. */
@Autonomous(name = "Auto Path Test", group = "Tests")
public final class AutoPathTest extends SelectableAutoOpMode {
    @Override protected AutoContext.Mode executionMode() { return AutoContext.Mode.PATH_TEST; }
    @Override protected boolean pathTestAdvanceDown() { return gamepad1.a; }
}
