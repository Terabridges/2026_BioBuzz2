package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

/**
 * Driver Station entry point "Auto Select": choose a catalog routine with D-pad
 * up/down, A to enter/confirm, and B to go back before Start. Choices are organized
 * by alliance, Garden/Loading side, and routine. Runs real actions in COMPETITION mode.
 * Menu/lifecycle logic is inherited; strategy logic belongs in autonomous/routines.
 */
@Autonomous(name = "Auto Select", group = "Auto")
public final class SelectableAuto extends SelectableAutoOpMode {}
