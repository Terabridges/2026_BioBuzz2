package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.bylazar.telemetry.JoinedTelemetry;
import com.bylazar.telemetry.PanelsTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.subsystems.Robot;

@TeleOp(name = "ShooterTunerPIDF", group = "Test")
public class shooterTunerPIDF extends OpMode {

    //Just make the PIDF variables public static on shooter subsystem

    Robot robot;

    Gamepad currentGamepad1;
    Gamepad previousGamepad1;

    Gamepad currentGamepad2;
    Gamepad previousGamepad2;

    private JoinedTelemetry joinedTelemetry;

    @Override
    public void init() {
        robot = new Robot(hardwareMap, telemetry);

        currentGamepad1 = new Gamepad();
        previousGamepad1 = new Gamepad();

        currentGamepad2 = new Gamepad();
        previousGamepad2 = new Gamepad();

        joinedTelemetry = new JoinedTelemetry(
                PanelsTelemetry.INSTANCE.getFtcTelemetry(),
                telemetry
        );

    }

    @Override
    public void start(){
        robot.outtake.shooter.init();
    }

    @Override
    public void loop() {
        gamepadUpdate();
        if (!previousGamepad1.a && currentGamepad1.a){
            robot.outtake.shooter.toggleUseFlywheel();
        }
        robot.outtake.shooter.update();

        joinedTelemetry.addData("Current RPM", robot.outtake.shooter.getCurrentRPM());
        joinedTelemetry.addData("Target RPM", robot.outtake.shooter.getTargetRPM());
        joinedTelemetry.addData("Current Power", robot.outtake.shooter.getCurrentPower());
        joinedTelemetry.addData("Use Flywheel", robot.outtake.shooter.getUseFlywheel());
        joinedTelemetry.update();
    }

    public void gamepadUpdate(){
        previousGamepad1.copy(currentGamepad1);
        currentGamepad1.copy(gamepad1);

        previousGamepad2.copy(currentGamepad2);
        currentGamepad2.copy(gamepad2);
    }
}
