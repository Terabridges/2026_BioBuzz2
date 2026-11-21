package org.firstinspires.ftc.teamcode.autonomous.paths;

import com.pedropathing.follower.Follower;
import com.pedropathing.paths.Path;
import java.util.function.LongSupplier;
import org.firstinspires.ftc.teamcode.actions.Action;

/** Follows a path without endpoint hold and stops if forward path progress stalls. */
public final class StallProtectedPathAction implements Action {
    private static final double MIN_PROGRESS_INCHES = 0.25;
    private static final long STALL_NANOS = 350_000_000L;

    private final Follower follower;
    private final Path path;
    private final LongSupplier nanoTime;
    private double remainingAtLastProgress;
    private long lastProgressTime;
    private boolean previousHoldEnd;
    private Result result = Result.READY;

    public StallProtectedPathAction(Follower follower, Path path) {
        this(follower, path, System::nanoTime);
    }

    StallProtectedPathAction(Follower follower, Path path, LongSupplier nanoTime) {
        this.follower = follower;
        this.path = path;
        this.nanoTime = nanoTime;
    }

    @Override public void start() {
        if (result == Result.RUNNING) stop();
        remainingAtLastProgress = Double.POSITIVE_INFINITY;
        lastProgressTime = nanoTime.getAsLong();
        previousHoldEnd = follower.holdEnd.get();
        follower.holdEnd.set(false);
        result = Result.RUNNING;
        follower.follow(path);
    }

    @Override public void update() {
        if (result != Result.RUNNING) return;
        if (!follower.isBusy()) {
            finish(Result.SUCCEEDED);
            return;
        }

        long now = nanoTime.getAsLong();
        double remaining = follower.remainingDistance();
        if (Double.isFinite(remaining) && remainingAtLastProgress - remaining >= MIN_PROGRESS_INCHES) {
            remainingAtLastProgress = remaining;
            lastProgressTime = now;
        } else if (now - lastProgressTime >= STALL_NANOS) {
            finish(Result.TIMED_OUT);
        }
    }

    @Override public Result result() { return result; }

    @Override public void stop() {
        if (result == Result.RUNNING) finish(Result.CANCELLED);
    }

    private void finish(Result outcome) {
        result = outcome;
        try {
            follower.stop();
        } finally {
            // Pedro cancels tracking on stop. Zero the motors now, even if this is the last loop.
            follower.holdEnd.set(previousHoldEnd);
            follower.drivetrain.stop();
        }
    }
}
