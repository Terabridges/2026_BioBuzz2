package org.firstinspires.ftc.teamcode.autonomous.routines;

import org.firstinspires.ftc.teamcode.autonomous.framework.AutoRoutine;

/**
 * Default strategy until a real auto is selected. It issues no commands and reports
 * success immediately on Start. Both menus keep this entry as their first choice.
 */
public final class DoNothingRoutine implements AutoRoutine {
    private Result result = Result.READY;

    @Override public void start() { result = Result.SUCCEEDED; }
    @Override public void update() {}
    @Override public void stop() {
        if (!isFinished()) result = Result.CANCELLED;
    }
    @Override public Result result() { return result; }
    @Override public String state() { return "Do Nothing"; }
}
