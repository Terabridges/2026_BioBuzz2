package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import org.firstinspires.ftc.teamcode.autonomous.framework.AutoCatalog;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoConfig;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoContext;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoMenu;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoRoutine;
import java.util.List;

/**
 * Shared nested menu for Auto Select and Auto Path Test. Choices come from AutoCatalog;
 * both menus construct the same routines, with the execution mode supplied in AutoContext.
 * Selection changes only during init_loop and is frozen when Start is pressed.
 * Keep menu behavior here; route execution belongs in the selected routine.
 */
public abstract class SelectableAutoOpMode extends BaseAutoOpMode {
    private final AutoMenu menu = new AutoMenu(AutoCatalog.ENTRIES);
    private boolean previousUp;
    private boolean previousDown;
    private boolean previousA;
    private boolean previousB;

    @Override protected final void resetSelection() {
        menu.reset();
        previousUp = false;
        previousDown = false;
        previousA = false;
        previousB = false;
    }

    @Override protected final boolean usesRobot() {
        if (executionMode() == AutoContext.Mode.PATH_TEST) return false;
        // Init precedes selection. Only initialize Robot if the competition menu needs it.
        return AutoCatalog.ENTRIES.stream().anyMatch(entry -> entry.requiresRobot);
    }

    @Override protected final void updateSelection() {
        if (gamepad1.b && !previousB) menu.back();
        else if (gamepad1.a && !previousA) menu.enter();
        else if (gamepad1.dpad_down && !previousDown) menu.move(1);
        else if (gamepad1.dpad_up && !previousUp) menu.move(-1);
        previousUp = gamepad1.dpad_up;
        previousDown = gamepad1.dpad_down;
        previousA = gamepad1.a;
        previousB = gamepad1.b;
    }

    @Override protected final AutoConfig selectedConfig() {
        return menu.selected().config;
    }

    @Override protected final AutoRoutine createRoutine(AutoContext context) {
        return menu.selected().create(context);
    }

    @Override protected final void addSelectionTelemetry() {
        telemetry.addData("Browsing", menu.location());
        List<String> options = menu.options();
        for (int i = 0; i < options.size(); i++) {
            telemetry.addLine((i == menu.cursor() ? "> " : "  ") + options.get(i));
        }
        telemetry.addLine("D-pad up/down: browse | A: enter/confirm | B: back");
        telemetry.addLine("Start runs the confirmed Auto shown above, not the highlighted folder.");
        if (executionMode() == AutoContext.Mode.PATH_TEST) {
            telemetry.addLine("Paths run normally; mechanism actions are skipped.");
        }
    }
}
