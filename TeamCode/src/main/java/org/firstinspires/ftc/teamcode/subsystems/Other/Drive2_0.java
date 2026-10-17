package org.firstinspires.ftc.teamcode.subsystems.Other;

import static org.firstinspires.ftc.robotcore.external.BlocksOpModeCompanion.gamepad1;
import static org.firstinspires.ftc.robotcore.external.BlocksOpModeCompanion.hardwareMap;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.follower.Follower;
import com.pedropathing.follower.ManualDrive;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.pedro.Constants;
import org.firstinspires.ftc.teamcode.pedro.FollowerManager;
import org.firstinspires.ftc.teamcode.subsystems.Subsystem;

public class Drive2_0 implements Subsystem {
    //---------Hardware--------------
    private Follower follower;

    //----------Software--------------
    public boolean isRobotCentric = false;
    public boolean isDriveOrHold = true;

    public Drive2_0(HardwareMap hardwareMap){
        follower = FollowerManager.getInstance(hardwareMap);
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
        return follower.isBusy();
    }


    //-----------Interface--------------
    @Override
    public void toInit() {

    }

    @Override
    public void update() {
        follower.update();
    }

    @Override
    public void stop() {
        follower.stop();
    }

    @Override
    public void logPsiKitData() {

    }
}
