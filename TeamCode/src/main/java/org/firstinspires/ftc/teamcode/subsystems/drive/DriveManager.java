package org.firstinspires.ftc.teamcode.subsystems.drive;

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

    public DriveManager(Backend backend) {
        this.backend = backend;
    }

    public void start(HardwareMap map, boolean resumeFromAuto) {
        if (started || stopped) throw new IllegalStateException("Drive session has already started or stopped");
        started = true;
        try {
            if (backend == Backend.PEDRO) {
                if (resumeFromAuto) manager = FollowerManager.forTeleOp(map);
                else {
                    FollowerManager.clearHandoff();
                    manager = new FollowerManager(map, Constants.create(map));
                }
                drive = new PedroDrive(manager.follower());
                manager.startManual();
            } else {
                FollowerManager.clearHandoff();
                direct = new DirectDrive(map);
                drive = direct;
            }
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
        if (started) {
            t.addData("Localization", manager == null ? "Direct drive"
                    : manager.resumedFromAuto() ? "From autonomous" : "Fresh follower");
        }
    }

    public Drive drive() {
        if (!started || stopped) throw new IllegalStateException("Drive is not active");
        return drive;
    }

    public void stop() {
        if (stopped) return;
        stopped = true;
        if (manager != null) manager.stop();
        if (direct != null) direct.stop();
    }
}
