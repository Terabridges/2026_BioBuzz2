package org.firstinspires.ftc.teamcode.actions;

import com.qualcomm.robotcore.util.ElapsedTime;
import java.util.function.BooleanSupplier;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Outtake;

/** Feeds a batch after spin-up and readiness. Ball count indicates feed completion, not scoring. */
public final class ShootAction implements Action {
    private static final double EMPTY_CONFIRM_SECONDS = 0.2;
    private enum Phase { WAITING_FOR_FEED, FEEDING, CONFIRMING_EMPTY }

    private final Outtake outtake;
    private final Intake intake;
    private final BooleanSupplier feedingAllowed;
    private final double timeoutSeconds;
    private final ElapsedTime timer = new ElapsedTime();
    private final ElapsedTime emptyTimer = new ElapsedTime();
    private Phase phase;
    private Result result = Result.READY;

    public ShootAction(Outtake outtake, Intake intake, BooleanSupplier feedingAllowed, double timeoutSeconds) {
        if (!Double.isFinite(timeoutSeconds) || timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Shooting requires a positive, finite timeout");
        }
        this.outtake = outtake;
        this.intake = intake;
        this.feedingAllowed = feedingAllowed;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override public void start() {
        timer.reset();
        phase = Phase.WAITING_FOR_FEED;
        result = Result.RUNNING;
    }

    @Override public void update() {
        if (result != Result.RUNNING) return;
        if (timer.seconds() >= timeoutSeconds) {
            finish(Result.TIMED_OUT);
            return;
        }

        if (outtake.isAutomaticFiring() && phase == Phase.WAITING_FOR_FEED) {
            phase = Phase.FEEDING;
        }
        if (phase != Phase.WAITING_FOR_FEED) {
            if (intake.spinner.getBallCount() > 0) {
                phase = Phase.FEEDING;
            } else if (phase == Phase.FEEDING) {
                phase = Phase.CONFIRMING_EMPTY;
                emptyTimer.reset();
            } else if (emptyTimer.seconds() >= EMPTY_CONFIRM_SECONDS) {
                finish(Result.SUCCEEDED);
                return;
            }
        }

        // Outtake owns the readiness checks and retains the feed latch across normal RPM dips.
        if (feedingAllowed.getAsBoolean()) outtake.requestFire();
        else outtake.stopFiring();
    }

    @Override public Result result() { return result; }
    @Override public void stop() {
        if (result == Result.RUNNING) finish(Result.CANCELLED);
        else if (result == Result.READY) result = Result.CANCELLED;
    }

    private void finish(Result outcome) {
        result = outcome;
        outtake.stopFiring();
        intake.stopFeed();
    }
}
