package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import org.firstinspires.ftc.teamcode.autonomous.framework.AutoCatalog;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoConfig;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoContext;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoRoutine;

/**
 * Shared D-pad menu for Auto Select and Auto Path Test. Choices come from AutoCatalog;
 * both menus construct the same routines, with the execution mode supplied in AutoContext.
 * Selection changes only during init_loop and is frozen when Start is pressed.
 * Keep menu behavior here; route execution belongs in the selected routine.
 */
public abstract class SelectableAutoOpMode extends BaseAutoOpMode {
    private int selected;
    private boolean previousUp;
    private boolean previousDown;

    @Override protected final void resetSelection() {
        selected = 0;
        previousUp = false;
        previousDown = false;
    }

    @Override protected final boolean usesRobot() {
        if (executionMode() == AutoContext.Mode.PATH_TEST) return false;
        // Init precedes selection. Only initialize Robot if the competition menu needs it.
        return AutoCatalog.ENTRIES.stream().anyMatch(entry -> entry.requiresRobot);
    }

    @Override protected final void updateSelection() {
        int count = AutoCatalog.ENTRIES.size();
        if (gamepad1.dpad_down && !previousDown) selected = (selected + 1) % count;
        else if (gamepad1.dpad_up && !previousUp) selected = (selected + count - 1) % count;
        previousUp = gamepad1.dpad_up;
        previousDown = gamepad1.dpad_down;
    }

    @Override protected final AutoConfig selectedConfig() {
        return AutoCatalog.ENTRIES.get(selected).config;
    }

    @Override protected final AutoRoutine createRoutine(AutoContext context) {
        return AutoCatalog.ENTRIES.get(selected).create(context);
    }

    @Override protected final void addSelectionTelemetry() {
        telemetry.addLine("D-pad up/down: select routine before Start");
        if (executionMode() == AutoContext.Mode.PATH_TEST) {
            telemetry.addLine("Paths run normally; mechanism actions are skipped.");
        }
    }
}
