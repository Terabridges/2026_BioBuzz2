package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * One owner per OpMode; only this manager updates/stops its follower.
 * Static storage is a stopped, one-time auto-to-TeleOp handoff, not a global follower cache.
 * A different HardwareMap or a new auto discards the old handoff.
 */
public final class FollowerManager {
    private static Handoff pending;

    private static final class Handoff {
        final HardwareMap map;
        final Follower follower;
        Handoff(HardwareMap map, Follower follower) {
            this.map = map;
            this.follower = follower;
        }
    }

    private final HardwareMap map;
    private final Follower follower;
    private final boolean resumedFromAuto;
    private boolean stopped;
    private boolean released;

    public FollowerManager(HardwareMap map, Follower follower) {
        this(map, follower, false);
    }

    private FollowerManager(HardwareMap map, Follower follower, boolean resumedFromAuto) {
        this.map = map;
        this.follower = Objects.requireNonNull(follower);
        this.resumedFromAuto = resumedFromAuto;
        // Cancel path/hold/manual commands, preserving localization and pose.
        follower.stop();
    }

    public static FollowerManager forTeleOp(HardwareMap map) {
        return forTeleOp(map, () -> Constants.create(map));
    }

    static synchronized FollowerManager forTeleOp(HardwareMap map, Supplier<Follower> fresh) {
        Handoff saved = pending;
        pending = null; // Taking a handoff transfers ownership exactly once.
        boolean resume = saved != null && saved.map == map;
        if (resume) restoreMotorSettings(map, saved.follower);
        return new FollowerManager(map, resume ? saved.follower : fresh.get(), resume);
    }

    private static void restoreMotorSettings(HardwareMap map, Follower follower) {
        if (!(follower.drivetrain instanceof Mecanum)) return;
        MecanumConfig config = ((Mecanum) follower.drivetrain).config;
        String[] names = {config.frontLeftName.get(), config.frontRightName.get(),
                config.backLeftName.get(), config.backRightName.get()};
        DcMotorSimple.Direction[] directions = {config.frontLeftDirection.get(),
                config.frontRightDirection.get(), config.backLeftDirection.get(),
                config.backRightDirection.get()};
        for (int i = 0; i < names.length; i++) {
            // The SDK resets hardware between OpModes; Pedro's CachedMotor retains its cache.
            // Restore raw hardware settings without rebuilding/resetting the localizer.
            DcMotor motor = map.get(DcMotor.class, names[i]);
            motor.setPower(0);
            motor.setDirection(directions[i]);
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            motor.setZeroPowerBehavior(config.manualBrakeMode.get()
                    ? DcMotor.ZeroPowerBehavior.BRAKE : DcMotor.ZeroPowerBehavior.FLOAT);
        }
    }

    public static synchronized void clearHandoff() { pending = null; }

    public Follower follower() {
        if (released) throw new IllegalStateException("Follower ownership was handed to TeleOp");
        return follower;
    }

    public boolean resumedFromAuto() { return resumedFromAuto; }

    /** Called at TeleOp Start, not Init, so no old autonomous command can resume. */
    public void startManual() {
        if (released || stopped) throw new IllegalStateException("Follower manager is no longer active");
        follower.manual(0, 0, 0);
    }

    public void update() {
        if (!stopped && !released) follower.update();
    }

    public void stop() {
        if (stopped || released) return;
        stopped = true;
        follower.stop();
    }

    /** Publish only after the auto's routine and mechanisms have stopped successfully. */
    public synchronized void handoffToTeleOp() {
        if (released) return;
        stop();
        synchronized (FollowerManager.class) {
            pending = new Handoff(map, follower);
        }
        released = true;
    }
}