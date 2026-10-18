package org.firstinspires.ftc.teamcode.subsystems.drive;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.follower.Follower;
import com.pedropathing.follower.ManualDrive;
import com.pedropathing.math.Pose;
import java.util.Objects;
/** Manual drive commands using an injected follower; the OpMode's manager owns updates. */
public class Drive2_0 {
    //---------Hardware--------------
    private final Follower follower;

    //----------Software--------------
    public boolean isRobotCentric = false;
    public boolean isDriveOrHold = true;

    public Drive2_0(Follower follower){
        this.follower = Objects.requireNonNull(follower);
    }
    //-----------Methods---------------
    public void robotCentric(double forward, double lateral, double turn) {

        follower.manual(forward, lateral, turn);
    }

    public void fieldCentric(double forward, double lateral, double turn) {
        DrivePowers powers = ManualDrive.fieldCentric(forward, lateral, turn, follower.pose().heading());
        follower.manual(powers);
    }

    public void driveOrHoldRobotCentric(double forward, double lateral, double turn) {
        ManualDrive.driveOrHold(follower, forward, lateral, turn);
    }

    public void driveOrHoldFieldCentric(double forward, double lateral, double turn) {
        DrivePowers powers = ManualDrive.fieldCentric(forward, lateral, turn, follower.pose().heading());
        ManualDrive.driveOrHold(follower, powers);
    }

    public Pose robotPose() {
        return follower.pose();
    }

    public void toggleCentricity() {
        isRobotCentric = !isRobotCentric;
    }

    public void toggleIsDriveOrHold() {
        isDriveOrHold = !isDriveOrHold;
    }
    public boolean isBusy() {
        return follower.following() && follower.isBusy();
    }
    public Follower getFollower() {
        return follower;
    }
}
