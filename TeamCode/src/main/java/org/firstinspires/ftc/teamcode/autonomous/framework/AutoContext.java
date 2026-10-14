package org.firstinspires.ftc.teamcode.autonomous.framework;

import com.pedropathing.follower.Follower;
import java.util.Objects;
import java.util.function.Function;
import org.firstinspires.ftc.teamcode.actions.Action;
import org.firstinspires.ftc.teamcode.subsystems.Robot;

/**
 * Dependencies passed to a routine for one run: configuration, follower, and mode.
 * Mechanism access goes through mechanismAction. In COMPETITION its factory receives
 * Robot and creates the real action; in PATH_TEST the factory is not called and a
 * replacement action completes immediately when started.
 * Both modes use the same routine and paths. Sensor-dependent branches need an
 * explicit test behavior when introduced; skipping actions does not simulate sensors.
 */
public final class AutoContext {
    public enum Mode { COMPETITION, PATH_TEST }
    public final AutoConfig config;
    public final Follower follower;
    public final Mode mode;
    private final Robot robot;

    public AutoContext(AutoConfig config, Follower follower, Robot robot, Mode mode) {
        this.config = Objects.requireNonNull(config);
        this.follower = Objects.requireNonNull(follower);
        this.mode = Objects.requireNonNull(mode);
        if (mode == Mode.PATH_TEST && robot != null) {
            throw new IllegalArgumentException("Path testing must not initialize mechanism hardware");
        }
        this.robot = robot;
    }

    /**
     * Create mechanism actions here with hardware access inside the factory.
     * PATH_TEST never calls the factory; its replacement completes on start.
     * Keep path completion checks separate so skipping mechanisms cannot skip driving.
     */
    public Action mechanismAction(Function<Robot, Action> factory) {
        Objects.requireNonNull(factory);
        if (mode == Mode.PATH_TEST) return new SkippedAction();
        if (robot == null) {
            throw new IllegalStateException("Register this routine with requiresRobot = true");
        }
        return Objects.requireNonNull(factory.apply(robot));
    }

    private static final class SkippedAction implements Action {
        private boolean finished;
        @Override public void start() { finished = true; }
        @Override public void update() {}
        @Override public boolean isFinished() { return finished; }
        @Override public void stop() { finished = true; }
    }
}
