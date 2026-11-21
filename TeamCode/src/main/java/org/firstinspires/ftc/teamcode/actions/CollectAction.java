package org.firstinspires.ftc.teamcode.actions;

import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;

/** Collects to an absolute ball count. The routine can run this alongside a path. */
public final class CollectAction implements Action {
    private final Intake intake;
    private final int targetCount;
    private final double timeoutSeconds;
    private final ElapsedTime timer = new ElapsedTime();
    private Result result = Result.READY;

    public CollectAction(Intake intake, int targetCount, double timeoutSeconds) {
        if (targetCount < 1 || targetCount > 4 || !Double.isFinite(timeoutSeconds) || timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Collection needs a target of 1–4 balls and a positive, finite timeout");
        }
        this.intake = intake;
        this.targetCount = targetCount;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override public void start() {
        timer.reset();
        result = Result.RUNNING;
        intake.collect();
    }

    @Override public void update() {
        if (result != Result.RUNNING) return;
        if (intake.spinner.getBallCount() >= targetCount) finish(Result.SUCCEEDED);
        else if (timer.seconds() >= timeoutSeconds) finish(Result.TIMED_OUT);
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
