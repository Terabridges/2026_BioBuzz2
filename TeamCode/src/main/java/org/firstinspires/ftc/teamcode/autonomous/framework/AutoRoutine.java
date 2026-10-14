package org.firstinspires.ftc.teamcode.autonomous.framework;

/**
 * Contract for a complete autonomous strategy. start begins the sequence, update
 * advances it without blocking, and stop cancels/cleans up active actions.
 * state and result expose progress to the common OpMode telemetry.
 * The OpMode updates the follower and Robot; the routine issues commands and checks
 * completion without updating those objects a second time.
 */
public interface AutoRoutine {
    enum Result { READY, RUNNING, SUCCEEDED, TIMED_OUT, CANCELLED }

    void start();
    void update();
    void stop();
    Result result();
    String state();

    default boolean isFinished() {
        return result() == Result.SUCCEEDED || result() == Result.TIMED_OUT
                || result() == Result.CANCELLED;
    }
}
