package org.firstinspires.ftc.teamcode.subsystems.drive;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.follower.Follower;
import com.pedropathing.follower.ManualDrive;
import com.pedropathing.math.Pose;
import java.util.Objects;

/** Commands only; FollowerManager owns follower updates and shutdown. */
public final class PedroDrive implements Drive {
    private final Follower follower;

    public PedroDrive(Follower follower) { this.follower = Objects.requireNonNull(follower); }

    @Override public void robotCentric(double forward, double right, double turn) {
        follower.manual(forward, -right, -turn);
    }

    @Override public void fieldCentric(double forward, double right, double turn) {
        DrivePowers powers = ManualDrive.fieldCentric(forward, -right, -turn, follower.pose().heading());
        follower.manual(powers);
    }

    public void driveOrHoldRobotCentric(double forward, double right, double turn) {
        ManualDrive.driveOrHold(follower, forward, -right, -turn);
    }

    public void driveOrHoldFieldCentric(double forward, double right, double turn) {
        DrivePowers powers = ManualDrive.fieldCentric(forward, -right, -turn, follower.pose().heading());
        ManualDrive.driveOrHold(follower, powers);
    }

    public boolean isBusy() { return follower.following() && follower.isBusy(); }
    public Pose robotPose() { return follower.pose(); }
}
