package org.firstinspires.ftc.teamcode.actions;

import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;

/** Collects to an absolute ball count. The routine can run this alongside a path. */
public final class CollectAction implements Action {
    private final Intake intake;
    private final int targetCount;
    private final double timeoutSeconds;
    private final ElapsedTime timer = new ElapsedTime();
    private final ElapsedTime fullTimer = new ElapsedTime();
    private final double fullDelaySeconds;
    private boolean targetReached;
    private Result result = Result.READY;

    public CollectAction(Intake intake, int targetCount, double timeoutSeconds) {
        this(intake, targetCount, timeoutSeconds, 0);
    }

    public CollectAction(Intake intake, int targetCount, double timeoutSeconds, double fullDelaySeconds) {
        if (targetCount < 1 || targetCount > 4 || !Double.isFinite(timeoutSeconds) || timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Collection needs a target of 1–4 balls and a positive, finite timeout");
        }
        if (!Double.isFinite(fullDelaySeconds) || fullDelaySeconds < 0) {
            throw new IllegalArgumentException("Collection delay must be finite and nonnegative");
        }
        this.intake = intake;
        this.targetCount = targetCount;
        this.timeoutSeconds = timeoutSeconds;
        this.fullDelaySeconds = fullDelaySeconds;
    }

    @Override public void start() {
        timer.reset();
        targetReached = false;
        result = Result.RUNNING;
        intake.collect();
    }

    @Override public void update() {
        if (result != Result.RUNNING) return;
        if (!targetReached && intake.spinner.getBallCount() >= targetCount) {
            targetReached = true;
            fullTimer.reset();
        }
        if (targetReached) {
            if (fullTimer.seconds() >= fullDelaySeconds) finish(Result.SUCCEEDED);
        } else if (timer.seconds() >= timeoutSeconds) finish(Result.TIMED_OUT);
    }

    @Override public Result result() { return result; }
    @Override public void stop() {
        if (result == Result.RUNNING) finish(Result.CANCELLED);
        else if (result == Result.READY) result = Result.CANCELLED;
    }

    private void finish(Result outcome) {
        result = outcome;
        intake.stopCollecting();
    }
}
