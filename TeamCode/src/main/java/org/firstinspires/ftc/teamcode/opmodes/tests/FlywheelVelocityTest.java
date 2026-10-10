package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.arcrobotics.ftclib.controller.PIDFController;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import org.psilynx.psikit.ftc.autolog.PsiKitAutoLog;

@PsiKitAutoLog(rlogPort = 5802)
@TeleOp(name = "Flywheel Velocity PID Test", group = "Tests")
public class FlywheelVelocityTest extends LinearOpMode {
    private static final double COARSE_RPM_STEP = 50.0;
    private static final double FINE_RPM_STEP = 10.0;
    private static final double ENCODER_TICKS_PER_REV = 28.0;
    private static final double PID_P = 0.0015;
    private static final double PID_I = 0.0001;
    private static final double PID_D = 0.0;
    private static final double PID_F = 0.0002;
    private static final double PID_INTEGRATION_LIMIT = 250.0;

    @Override
    public void runOpMode() {
        DcMotorEx leftFlywheel = hardwareMap.get(DcMotorEx.class, "fly_left");
        DcMotorEx rightFlywheel = hardwareMap.get(DcMotorEx.class, "fly_right");

        boolean leftReversed = true;
        leftFlywheel.setDirection(DcMotorSimple.Direction.REVERSE);
        rightFlywheel.setDirection(DcMotorSimple.Direction.FORWARD);
        leftFlywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rightFlywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        double targetRpm = 0.0;
        boolean previousDpadUp = false;
        boolean previousDpadDown = false;
        boolean previousDpadLeft = false;
        boolean previousDpadRight = false;
        boolean previousA = false;

        telemetry.addLine("D-pad up/down: +/-50 RPM | left/right: +/-10 RPM");
        telemetry.addLine("A: toggle left motor direction at 0 RPM target");
        telemetry.addLine("Left encoder controls shared power to both motors");
        telemetry.update();

        waitForStart();
        PIDFController flywheelPID = new PIDFController(PID_P, PID_I, PID_D, PID_F);
        flywheelPID.setIntegrationBounds(-PID_INTEGRATION_LIMIT, PID_INTEGRATION_LIMIT);
        flywheelPID.setTolerance(150.0);

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

                double encoderRpm = Math.abs(leftFlywheel.getVelocity())
                        * 60.0 / ENCODER_TICKS_PER_REV;
                double power;
                if (targetRpm == 0.0) {
                    flywheelPID.reset();
                    power = 0.0;
                } else {
                    power = clip(flywheelPID.calculate(encoderRpm, targetRpm), 0.0, 1.0);
                }

                leftFlywheel.setPower(power);
                rightFlywheel.setPower(power);

                telemetry.addData("Target RPM", "%.0f", targetRpm);
                telemetry.addData("Encoder RPM (left)", "%.0f", encoderRpm);
                telemetry.addData("Configured ticks/rev", "%.1f",
                        leftFlywheel.getMotorType().getTicksPerRev());
                telemetry.addData("Shared motor power", "%.3f", power);
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

    private static double clip(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}