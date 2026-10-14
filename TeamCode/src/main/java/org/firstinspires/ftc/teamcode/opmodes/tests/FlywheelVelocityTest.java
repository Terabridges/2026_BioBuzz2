package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

@TeleOp(name = "Flywheel Raw Encoder Test", group = "Tests")
public class FlywheelVelocityTest extends LinearOpMode {
    private static final double TEST_POWER = 0.15;
    private static final double ENCODER_TICKS_PER_REV = 28.0;
    private static final long RPM_SAMPLE_INTERVAL_NANOS = 250_000_000L;

    @Override
    public void runOpMode() {
        DcMotorEx leftFlywheel = hardwareMap.get(DcMotorEx.class, "flywheel_left");
        DcMotorEx rightFlywheel = hardwareMap.get(DcMotorEx.class, "flywheel_right");

        leftFlywheel.setDirection(DcMotorSimple.Direction.REVERSE);
        rightFlywheel.setDirection(DcMotorSimple.Direction.FORWARD);
        leftFlywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rightFlywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        telemetry.addLine("Hold LB to jog left motor; hold RB to jog right motor.");
        telemetry.addLine("Use one bumper at a time. Power is limited to 0.15.");
        telemetry.addLine("Release both bumpers to stop.");
        telemetry.update();

        waitForStart();
        int previousLeftPosition = leftFlywheel.getCurrentPosition();
        int previousRightPosition = rightFlywheel.getCurrentPosition();
        long previousSampleTime = System.nanoTime();
        int leftTicksPerSample = 0;
        int rightTicksPerSample = 0;
        double leftRpm = 0.0;
        double rightRpm = 0.0;

        try {
            while (opModeIsActive()) {
                boolean testLeft = gamepad1.left_bumper && !gamepad1.right_bumper;
                boolean testRight = gamepad1.right_bumper && !gamepad1.left_bumper;

                leftFlywheel.setPower(testLeft ? TEST_POWER : 0.0);
                rightFlywheel.setPower(testRight ? TEST_POWER : 0.0);

                int leftEncoderPosition = leftFlywheel.getCurrentPosition();
                int rightEncoderPosition = rightFlywheel.getCurrentPosition();
                long sampleTime = System.nanoTime();
                long elapsedNanos = sampleTime - previousSampleTime;
                if (elapsedNanos >= RPM_SAMPLE_INTERVAL_NANOS) {
                    double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
                    leftTicksPerSample = leftEncoderPosition - previousLeftPosition;
                    rightTicksPerSample = rightEncoderPosition - previousRightPosition;
                    leftRpm = Math.abs(leftTicksPerSample) * 60.0
                        / (ENCODER_TICKS_PER_REV * elapsedSeconds);
                    rightRpm = Math.abs(rightTicksPerSample) * 60.0
                        / (ENCODER_TICKS_PER_REV * elapsedSeconds);
                    previousLeftPosition = leftEncoderPosition;
                    previousRightPosition = rightEncoderPosition;
                    previousSampleTime = sampleTime;
                }

                telemetry.addData("Test", testLeft ? "LEFT" : testRight ? "RIGHT" : "STOPPED");
                telemetry.addData("Left connection", leftFlywheel.getConnectionInfo());
                telemetry.addData("Left position", leftEncoderPosition);
                telemetry.addData("Left ticks/sample, RPM", "%d, %.1f", leftTicksPerSample, leftRpm);
                telemetry.addData("Left velocity (ticks/sec)", "%.1f", leftFlywheel.getVelocity());
                telemetry.addData("Right connection", rightFlywheel.getConnectionInfo());
                telemetry.addData("Right position", rightEncoderPosition);
                telemetry.addData("Right ticks/sample, RPM", "%d, %.1f", rightTicksPerSample, rightRpm);
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