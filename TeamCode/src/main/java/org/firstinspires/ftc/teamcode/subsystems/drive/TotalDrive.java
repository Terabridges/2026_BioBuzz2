package org.firstinspires.ftc.teamcode.subsystems.drive;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;

public class TotalDrive implements Subsystem {
    public enum DriveMode {
        PEDRO_PATHING,
        RAW_TELEOP
    }

    private DriveMode currentMode =  DriveMode.RAW_TELEOP;
    private Drive rawDrive;
    private Drive2_0 pedroDrive;
    private boolean fieldCentric = false;

    public TotalDrive(HardwareMap map) {
        this.rawDrive = new Drive(map);
        this.pedroDrive = new Drive2_0(map);
    }

    public void setMode(DriveMode mode) {
        if (this.currentMode != mode) {
            // Clean up old state before switching
            if (this.currentMode == DriveMode.PEDRO_PATHING) {
                pedroDrive.getFollower().stop();
            } else {
                rawDrive.stop();
            }
            this.currentMode = mode;
        }
    }

    public DriveMode getMode() {
        return currentMode;
    }

    public void toggleFieldCentric() {
        this.fieldCentric = !this.fieldCentric;
        rawDrive.useFieldCentric = this.fieldCentric;
        pedroDrive.isRobotCentric = !this.fieldCentric;
    }

    public boolean isFieldCentric() {
        return fieldCentric;
    }

    public void drive(double forward, double strafe, double turn) {
        switch (currentMode) {
            case PEDRO_PATHING:
                if (pedroDrive.isRobotCentric) {
                    pedroDrive.robotCentric(forward, strafe, turn);
                } else {
                    pedroDrive.fieldCentric(forward, strafe, turn);
                }
                break;

            case RAW_TELEOP:
                if (rawDrive.useFieldCentric) {
                    rawDrive.driveFieldRelative(forward, strafe, turn);
                } else {
                    rawDrive.drive(forward, strafe, turn);
                }
                break;
        }
    }

    @Override
    public void update() {
        switch (currentMode) {
            case PEDRO_PATHING:
                pedroDrive.update();
                break;

            case RAW_TELEOP:
                rawDrive.update();
                break;
        }
    }

    @Override
    public void init() {
        rawDrive.init();
        pedroDrive.init();
    }

    @Override
    public void stop() {
        rawDrive.stop();
        pedroDrive.stop();
    }

    @Override
    public void logPsiKitData() {
        rawDrive.logPsiKitData();
        pedroDrive.logPsiKitData();
    }

    public Drive getRawDrive() {
        return rawDrive;
    }

    public Drive2_0 getPedroDrive() {
        return pedroDrive;
    }
}
