package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

@TeleOp(name = "Flywheel Velocity PID Test", group = "Tests")
public class FlywheelVelocityTest extends LinearOpMode {
    private static final double COARSE_RPM_STEP = 50.0;
    private static final double FINE_RPM_STEP = 10.0;

    @Override
    public void runOpMode() {
        DcMotorEx leftFlywheel = hardwareMap.get(DcMotorEx.class, "fly_left");
        DcMotorEx rightFlywheel = hardwareMap.get(DcMotorEx.class, "fly_right");

        boolean leftReversed = true;
        leftFlywheel.setDirection(DcMotorSimple.Direction.REVERSE);
        rightFlywheel.setDirection(DcMotorSimple.Direction.FORWARD);
        leftFlywheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightFlywheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        double targetRpm = 0.0;
        boolean previousDpadUp = false;
        boolean previousDpadDown = false;
        boolean previousDpadLeft = false;
        boolean previousDpadRight = false;
        boolean previousA = false;

        telemetry.addLine("D-pad up/down: +/-50 RPM | left/right: +/-10 RPM");
        telemetry.addLine("A: toggle left motor direction at 0 RPM target");
        telemetry.update();

        waitForStart();

        try {
            while (opModeIsActive()) {
                if (gamepad1.dpad_up && !previousDpadUp) {
                    targetRpm += COARSE_RPM_STEP;
                }
                if (gamepad1.dpad_down && !previousDpadDown) {
                    targetRpm = Math.max(0.0, targetRpm - COARSE_RPM_STEP);
                }
                if (gamepad1.dpad_right && !previousDpadRight) {
                    targetRpm += FINE_RPM_STEP;
                }
                if (gamepad1.dpad_left && !previousDpadLeft) {
                    targetRpm = Math.max(0.0, targetRpm - FINE_RPM_STEP);
                }

                if (gamepad1.a && !previousA && targetRpm == 0.0) {
                    leftReversed = !leftReversed;
                    leftFlywheel.setDirection(leftReversed
                            ? DcMotorSimple.Direction.REVERSE
                            : DcMotorSimple.Direction.FORWARD);
                }

                previousDpadUp = gamepad1.dpad_up;
                previousDpadDown = gamepad1.dpad_down;
                previousDpadLeft = gamepad1.dpad_left;
                previousDpadRight = gamepad1.dpad_right;
                previousA = gamepad1.a;

                double leftTicksPerSecond = targetRpm
                        * leftFlywheel.getMotorType().getTicksPerRev() / 60.0;
                double rightTicksPerSecond = targetRpm
                        * rightFlywheel.getMotorType().getTicksPerRev() / 60.0;
                leftFlywheel.setVelocity(leftTicksPerSecond);
                rightFlywheel.setVelocity(rightTicksPerSecond);

                double leftRpm = leftFlywheel.getVelocity()
                        * 60.0 / leftFlywheel.getMotorType().getTicksPerRev();
                double rightRpm = rightFlywheel.getVelocity()
                        * 60.0 / rightFlywheel.getMotorType().getTicksPerRev();

                telemetry.addData("Target RPM", "%.0f", targetRpm);
                telemetry.addData("Left RPM", "%.0f", leftRpm);
                telemetry.addData("Right RPM", "%.0f", rightRpm);
                telemetry.addData("Left direction", leftReversed ? "REVERSE" : "FORWARD");
                telemetry.addData("Direction toggle", targetRpm == 0.0
                        ? "Press A"
                        : "Set target to 0 RPM first");
                telemetry.update();

                idle();
            }
        } finally {
            leftFlywheel.setPower(0.0);
            rightFlywheel.setPower(0.0);
        }
    }
}