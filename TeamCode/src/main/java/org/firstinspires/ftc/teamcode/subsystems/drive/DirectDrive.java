package org.firstinspires.ftc.teamcode.subsystems.drive;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/** Direct wheel-power mixing. Robot-centric mode requires only the four drive motors. */
public final class DirectDrive implements Drive {
    private final HardwareMap map;
    private final DcMotor frontLeft, frontRight, backLeft, backRight;
    private GoBildaPinpointDriver pinpoint;

    public DirectDrive(HardwareMap map) {
        this.map = map;
        frontLeft = map.get(DcMotor.class, "front_left");
        frontRight = map.get(DcMotor.class, "front_right");
        backLeft = map.get(DcMotor.class, "back_left");
        backRight = map.get(DcMotor.class, "back_right");
        frontLeft.setDirection(DcMotor.Direction.FORWARD);
        backLeft.setDirection(DcMotor.Direction.FORWARD);
        frontRight.setDirection(DcMotor.Direction.REVERSE);
        backRight.setDirection(DcMotor.Direction.REVERSE);
        for (DcMotor motor : new DcMotor[]{frontLeft, frontRight, backLeft, backRight}) {
            motor.setPower(0);
            motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
    }

    @Override public void robotCentric(double forward, double right, double turn) {
        double fl = forward + right + turn;
        double fr = forward - right - turn;
        double bl = forward - right + turn;
        double br = forward + right - turn;
        double scale = Math.max(1, Math.max(Math.max(Math.abs(fl), Math.abs(fr)),
                Math.max(Math.abs(bl), Math.abs(br))));
        frontLeft.setPower(fl / scale);
        frontRight.setPower(fr / scale);
        backLeft.setPower(bl / scale);
        backRight.setPower(br / scale);
    }

    @Override public void fieldCentric(double forward, double right, double turn) {
        if (pinpoint == null) pinpoint = map.get(GoBildaPinpointDriver.class, "pinpoint");
        pinpoint.update(GoBildaPinpointDriver.ReadData.ONLY_UPDATE_HEADING);
        fieldCentricAtHeading(forward, right, turn, pinpoint.getHeading(AngleUnit.RADIANS));
    }

    void fieldCentricAtHeading(double forward, double right, double turn, double heading) {
        robotCentric(forward * Math.cos(heading) - right * Math.sin(heading),
                right * Math.cos(heading) + forward * Math.sin(heading), turn);
    }

    public void stop() { robotCentric(0, 0, 0); }
}
