package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

@TeleOp(name = "Flywheel Encoder Diagnostic", group = "Tests")
public class FlywheelEncoderDiagnostic extends LinearOpMode {
    private static final double TEST_POWER = 0.15;

    @Override
    public void runOpMode() {
        DcMotorEx leftFlywheel = hardwareMap.get(DcMotorEx.class, "fly_left");
        DcMotorEx rightFlywheel = hardwareMap.get(DcMotorEx.class, "fly_right");

        leftFlywheel.setDirection(DcMotorSimple.Direction.REVERSE);
        rightFlywheel.setDirection(DcMotorSimple.Direction.FORWARD);
        leftFlywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rightFlywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        leftFlywheel.setPower(0.0);
        rightFlywheel.setPower(0.0);

        telemetry.addLine("Hold LB to jog left motor; hold RB to jog right motor.");
        telemetry.addLine("Use one bumper at a time. Test power is limited to 0.15.");
        telemetry.addLine("Release both bumpers to stop.");
        telemetry.update();

        waitForStart();

        try {
            while (opModeIsActive()) {
                boolean testLeft = gamepad1.left_bumper && !gamepad1.right_bumper;
                boolean testRight = gamepad1.right_bumper && !gamepad1.left_bumper;

                leftFlywheel.setPower(testLeft ? TEST_POWER : 0.0);
                rightFlywheel.setPower(testRight ? TEST_POWER : 0.0);

                telemetry.addData("Test", testLeft ? "LEFT" : testRight ? "RIGHT" : "STOPPED");
                telemetry.addData("Left connection", leftFlywheel.getConnectionInfo());
                telemetry.addData("Left position", leftFlywheel.getCurrentPosition());
                telemetry.addData("Left velocity (ticks/sec)", "%.1f", leftFlywheel.getVelocity());
                telemetry.addData("Right connection", rightFlywheel.getConnectionInfo());
                telemetry.addData("Right position", rightFlywheel.getCurrentPosition());
                telemetry.addData("Right velocity (ticks/sec)", "%.1f", rightFlywheel.getVelocity());
                telemetry.update();

                idle();
            }
        } finally {
            leftFlywheel.setPower(0.0);
            rightFlywheel.setPower(0.0);
        }
    }
}