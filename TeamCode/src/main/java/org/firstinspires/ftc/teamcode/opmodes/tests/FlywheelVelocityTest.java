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
        // Same motor and direction as Shooter, so positive velocity here means positive RPM there
        DcMotorEx flywheel = hardwareMap.get(DcMotorEx.class, "flywheel");

        flywheel.setDirection(DcMotorSimple.Direction.FORWARD);
        flywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        telemetry.addLine("Hold RB to jog the flywheel. Power is limited to 0.15.");
        telemetry.addLine("Release RB to stop.");
        telemetry.update();

        waitForStart();
        int previousPosition = flywheel.getCurrentPosition();
        long previousSampleTime = System.nanoTime();
        int ticksPerSample = 0;
        double rpm = 0.0;

        try {
            while (opModeIsActive()) {
                boolean testing = gamepad1.right_bumper;

                flywheel.setPower(testing ? TEST_POWER : 0.0);

                // RPM worked out from the position change, to check it against getVelocity()
                int encoderPosition = flywheel.getCurrentPosition();
                long sampleTime = System.nanoTime();
                long elapsedNanos = sampleTime - previousSampleTime;
                if (elapsedNanos >= RPM_SAMPLE_INTERVAL_NANOS) {
                    double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
                    ticksPerSample = encoderPosition - previousPosition;
                    rpm = Math.abs(ticksPerSample) * 60.0
                        / (ENCODER_TICKS_PER_REV * elapsedSeconds);
                    previousPosition = encoderPosition;
                    previousSampleTime = sampleTime;
                }

                telemetry.addData("Test", testing ? "RUNNING" : "STOPPED");
                telemetry.addData("Connection", flywheel.getConnectionInfo());
                telemetry.addData("Position", encoderPosition);
                telemetry.addData("Ticks/sample, RPM", "%d, %.1f", ticksPerSample, rpm);
                telemetry.addData("Velocity (ticks/sec)", "%.1f", flywheel.getVelocity());
                telemetry.update();

                idle();
            }
        } finally {
            flywheel.setPower(0.0);
        }
    }
}