/**
 * Autonomous organization:
 *
 * Classes in framework provide the common autonomous infrastructure.
 * AutoCatalog lists the strategies available in both Driver Station menus.
 * AutoMenu groups choices by alliance, Garden/Loading side, and routine.
 * AutoConfig holds those selected settings and starting pose; AutoContext supplies
 * the follower and mechanism-action factory for the active run.
 *
 * FieldPoses defines locations. Classes in paths connect those locations with
 * Pedro paths. Classes in routines decide when to drive, collect, score, and finish.
 * Each routine owns its sequence or state machine; reusable mechanism behaviors
 * belong in actions, and hardware/control loops belong in subsystems.
 *
 * BaseAutoOpMode owns init/start/loop/stop, updates FollowerManager and Robot,
 * and offers the stopped auto follower to TeleOp through a one-time handoff.
 * PedroDrive and Robot never create or update a Pedro follower themselves.
 * SelectableAuto runs the selected strategy normally. AutoPathTest runs the same
 * strategy in PATH_TEST mode, skipping actions created through mechanismAction.
 * Path testing verifies movement, not mechanism timing or sensor-driven decisions.
 */
package org.firstinspires.ftc.teamcode.autonomous;
