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
        // Same motor and direction as Shooter, so positive velocity here means positive RPM there
        DcMotorEx flywheel = hardwareMap.get(DcMotorEx.class, "flywheel");

        flywheel.setDirection(DcMotorSimple.Direction.FORWARD);
        flywheel.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        flywheel.setPower(0.0);

        telemetry.addLine("Hold RB to jog the flywheel. Test power is limited to 0.15.");
        telemetry.addLine("Release RB to stop.");
        telemetry.update();

        waitForStart();

        try {
            while (opModeIsActive()) {
                boolean testing = gamepad1.right_bumper;

                flywheel.setPower(testing ? TEST_POWER : 0.0);

                telemetry.addData("Test", testing ? "RUNNING" : "STOPPED");
                telemetry.addData("Connection", flywheel.getConnectionInfo());
                telemetry.addData("Position", flywheel.getCurrentPosition());
                telemetry.addData("Velocity (ticks/sec)", "%.1f", flywheel.getVelocity());
                telemetry.update();

                idle();
            }
        } finally {
            flywheel.setPower(0.0);
        }
    }
}