package org.firstinspires.ftc.teamcode.actions;

import com.sfdev.assembly.state.StateMachine;
import com.sfdev.assembly.state.StateMachineBuilder;
import com.qualcomm.robotcore.util.ElapsedTime;
import java.util.function.BooleanSupplier;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Outtake;

/** Feeds a batch after spin-up and readiness. Ball count indicates feed completion, not scoring. */
public final class ShootAction implements Action {
    private static final double EMPTY_CONFIRM_SECONDS = 0.2;
    private enum Phase { WAITING_FOR_FEED, FEEDING, CONFIRMING_EMPTY, WAITING_FOR_CLEAR_PULSE, CLEARING_FEED, DONE }

    private final Outtake outtake;
    private final Intake intake;
    private final BooleanSupplier feedingAllowed;
    private final double timeoutSeconds;
    private final ElapsedTime timer = new ElapsedTime();
    private final ElapsedTime emptyTimer = new ElapsedTime();
    private final StateMachine machine;
    private Result result = Result.READY;

    public ShootAction(Outtake outtake, Intake intake, BooleanSupplier feedingAllowed, double timeoutSeconds) {
        if (!Double.isFinite(timeoutSeconds) || timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Shooting requires a positive, finite timeout");
        }
        this.outtake = outtake;
        this.intake = intake;
        this.feedingAllowed = feedingAllowed;
        this.timeoutSeconds = timeoutSeconds;
        machine = new StateMachineBuilder()
                .state(Phase.WAITING_FOR_FEED)
                .transition(() -> outtake.isAutomaticFiring() && !hasBalls(), Phase.CONFIRMING_EMPTY,
                        emptyTimer::reset)
                .transition(outtake::isAutomaticFiring, Phase.FEEDING)
                .state(Phase.FEEDING)
                .transition(() -> !hasBalls(), Phase.CONFIRMING_EMPTY, emptyTimer::reset)
                .state(Phase.CONFIRMING_EMPTY)
                .transition(this::hasBalls, Phase.FEEDING)
                .transition(() -> emptyTimer.seconds() >= EMPTY_CONFIRM_SECONDS && isBetweenPulses(),
                        Phase.WAITING_FOR_CLEAR_PULSE)
                .state(Phase.WAITING_FOR_CLEAR_PULSE)
                .transition(outtake::shouldRunFeedMotors, Phase.CLEARING_FEED)
                .state(Phase.CLEARING_FEED)
                .transition(() -> isBetweenPulses() && hasBalls(), Phase.FEEDING)
                .transition(this::isBetweenPulses, Phase.DONE)
                .state(Phase.DONE)
                .build();
    }

    private boolean hasBalls() { return intake.spinner.getBallCount() > 0; }

    private boolean isBetweenPulses() {
        return outtake.isAutomaticFiring() && !outtake.shouldRunFeedMotors();
    }

    @Override public void start() {
        timer.reset();
        machine.setState(Phase.WAITING_FOR_FEED);
        machine.start();
        result = Result.RUNNING;
    }

    @Override public void update() {
        if (result != Result.RUNNING) return;
        if (timer.seconds() >= timeoutSeconds) {
            finish(Result.TIMED_OUT);
            return;
        }

        machine.update();
        if (machine.getState() == Phase.DONE) {
            finish(Result.SUCCEEDED);
            return;
        }

        if (feedingAllowed.getAsBoolean()) {
            // A ball can leave the count sensors before it clears the transfer.
            outtake.requestPulsedFire(machine.getState() == Phase.WAITING_FOR_CLEAR_PULSE
                    || machine.getState() == Phase.CLEARING_FEED);
        } else {
            outtake.stopFiring();
        }
    }

    @Override public Result result() { return result; }
    @Override public void stop() {
        if (result == Result.RUNNING) finish(Result.CANCELLED);
        else if (result == Result.READY) result = Result.CANCELLED;
    }

    private void finish(Result outcome) {
        machine.stop();
        result = outcome;
        outtake.stopFiring();
        intake.stopFeed();
    }
}
