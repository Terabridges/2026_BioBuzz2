package org.firstinspires.ftc.teamcode.autonomous.routines;

import com.pedropathing.math.Velocity;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.actions.Action;
import org.firstinspires.ftc.teamcode.actions.EnableShootingAction;
import org.firstinspires.ftc.teamcode.actions.ShootAction;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoContext;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoRoutine;
import org.firstinspires.ftc.teamcode.utility.shooterUtility.TurretAim;

/** Stationary preload shot. The field placement must be in the chosen cell's shooting zone. */
public final class ShootPreloadRoutine implements AutoRoutine {
    private static final double SHOT_TIMEOUT_SECONDS = 5;
    private static final double SETTLE_SECONDS = 0.15;
    private static final double MAX_SETTLED_SPEED = 2.0; // inches per second
    private static final double MAX_SETTLED_TURN_RATE = Math.toRadians(5);

    private final AutoContext context;
    private final Action enableShooting;
    private final Action shoot;
    private final ElapsedTime settleTimer = new ElapsedTime();
    private Result result = Result.READY;

    public ShootPreloadRoutine(AutoContext context, TurretAim.Cell targetCell) {
        this.context = context;
        enableShooting = context.mechanismAction(robot ->
                new EnableShootingAction(robot.outtake, context.config.alliance, targetCell));
        shoot = context.mechanismAction(robot ->
                new ShootAction(robot.outtake, robot.intake, this::isSettled, SHOT_TIMEOUT_SECONDS));
    }

    @Override public void start() {
        settleTimer.reset();
        enableShooting.start();
        shoot.start();
        result = Result.RUNNING;
    }

    @Override public void update() {
        shoot.update();
        switch (shoot.result()) {
            case SUCCEEDED: result = Result.SUCCEEDED; break;
            case TIMED_OUT: result = Result.TIMED_OUT; break;
            case CANCELLED: result = Result.CANCELLED; break;
            default: break;
        }
    }

    private boolean isSettled() {
        Velocity velocity = context.follower.velocity();
        if (velocity == null || !(Math.hypot(velocity.vx, velocity.vy) <= MAX_SETTLED_SPEED
                && Math.abs(velocity.omega) <= MAX_SETTLED_TURN_RATE)) {
            settleTimer.reset();
            return false;
        }
        return settleTimer.seconds() >= SETTLE_SECONDS;
    }

    @Override public void stop() {
        shoot.stop();
        if (!isFinished()) result = Result.CANCELLED;
    }

    @Override public Result result() { return result; }
    @Override public String state() { return "Shoot preload"; }
}
