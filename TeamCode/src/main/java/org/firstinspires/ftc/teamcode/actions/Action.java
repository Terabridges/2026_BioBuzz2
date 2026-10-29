package org.firstinspires.ftc.teamcode.actions;

/**
 * Reusable mechanism behavior, such as a future collection or scoring action.
 * start issues initial commands, update checks/advances behavior without blocking,
 * isFinished reports completion, and stop cleans up commanded mechanism behavior.
 * The owning routine calls these methods and decides when to advance or recover.
 * Autonomous routines create these through AutoContext.mechanismAction so path tests
 * can skip mechanisms while preserving the route sequence.
 */
public interface Action {
    void start();
    void update();
    boolean isFinished();
    void stop();
}
