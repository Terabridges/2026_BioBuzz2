package org.firstinspires.ftc.teamcode.subsystems.drive;

import com.pedropathing.follower.Follower;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.pedro.Constants;
import org.firstinspires.ftc.teamcode.pedro.FollowerManager;

/** Owns one drivetrain and its lifecycle; gamepad mappings belong to control classes. */
public final class DriveManager {
    public enum Backend { DIRECT, PEDRO }
    private final Backend backend;
    private boolean started;
    private boolean stopped;
    private DirectDrive direct;
    private FollowerManager manager;
    private Drive drive;
    private GoBildaPinpointDriver pinpoint;

    public DriveManager(Backend backend) {
        this.backend = backend;
    }

    public void start(HardwareMap map, boolean resumeFromAuto) {
        initialize(map, resumeFromAuto);
        start();
    }

    /** Prepare the drive and its localizer while the robot is stationary during OpMode INIT. */
    public void initialize(HardwareMap map, boolean resumeFromAuto) {
        if (drive != null || stopped) throw new IllegalStateException("Drive session has already been initialized or stopped");
        try {
            if (backend == Backend.PEDRO) {
                if (resumeFromAuto) manager = FollowerManager.forTeleOp(map);
                else {
                    FollowerManager.clearHandoff();
                    manager = new FollowerManager(map, Constants.create(map));
                }
                drive = new PedroDrive(manager.follower());
            } else {
                FollowerManager.clearHandoff();
                direct = new DirectDrive(map);
                drive = direct;
            }
            // The same device both backends read (hardwareMap hands back the same object)
            pinpoint = map.get(GoBildaPinpointDriver.class, "pinpoint");
        } catch (RuntimeException failure) {
            stop();
            throw failure;
        }
    }

    public void start() {
        if (drive == null || started || stopped) throw new IllegalStateException("Drive session cannot start");
        try {
            if (manager != null) manager.startManual();
            started = true;
        } catch (RuntimeException failure) {
            stop();
            throw failure;
        }
    }

    public void update() {
        if (!started || stopped) return;
        try {
            if (manager != null) manager.update();
        } catch (RuntimeException failure) {
            stop();
            throw failure;
        }
    }

    public void addTelemetry(Telemetry t) {
        t.addData("Drive backend", backend);
        if (drive != null) {
            t.addData("Localization", manager == null ? "Direct drive"
                    : manager.resumedFromAuto() ? "From autonomous" : "Fresh follower");
        }
    }

    public Drive drive() {
        if (!started || stopped) throw new IllegalStateException("Drive is not active");
        return drive;
    }

    /** Pedro's follower, for reading and resetting the field pose. Only the PEDRO backend has one. */
    public Follower follower() {
        if (!started || stopped) throw new IllegalStateException("Drive is not active");
        if (manager == null) throw new IllegalStateException("Only the PEDRO backend has a follower");
        return manager.follower();
    }

    /**
     * Recalibrates the Pinpoint's gyro, like its button but without zeroing the pose. The robot MUST sit still
     * for about 0.25 s while it samples the new zero. This stops future drift; it doesn't undo heading error
     * that has already built up (a pose reset does that).
     */
    public void recalibrateImu() {
        if (!started || stopped) throw new IllegalStateException("Drive is not active");
        pinpoint.recalibrateIMU();
    }

    /** True when the pose came from autonomous, so it shouldn't be overwritten with a start pose. */
    public boolean resumedFromAuto() {
        return manager != null && manager.resumedFromAuto();
    }

    public void stop() {
        if (stopped) return;
        stopped = true;
        if (manager != null) manager.stop();
        if (direct != null) direct.stop();
    }
}
