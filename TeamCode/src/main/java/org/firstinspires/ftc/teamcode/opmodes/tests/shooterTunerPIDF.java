package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.bylazar.telemetry.JoinedTelemetry;
import com.bylazar.telemetry.PanelsTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Shooter;

@TeleOp(name = "ShooterTunerPIDF", group = "Test")
public class shooterTunerPIDF extends OpMode {

    //Just make the PIDF variables public static on shooter subsystem

    Robot robot;

    Gamepad currentGamepad1;
    Gamepad previousGamepad1;

    Gamepad currentGamepad2;
    Gamepad previousGamepad2;

    private JoinedTelemetry joinedTelemetry;

    private static final String[] PARAM_NAMES = {"Target RPM", "P", "I", "D", "F"};
    private final double[] steps = {100, 0.0001, 0.00001, 0.00001, 0.00001};
    private int selected = 0;

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
        gamepadTune();
        robot.outtake.shooter.update();

        joinedTelemetry.addData("Editing", PARAM_NAMES[selected] + "  (step " + steps[selected] + ")");
        joinedTelemetry.addData("Target / P / I / D / F", "%.0f / %.6f / %.6f / %.6f / %.6f",
                Shooter.flywheelTargetRPM, Shooter.p, Shooter.i, Shooter.d, Shooter.f);
        joinedTelemetry.addData("Current RPM", robot.outtake.shooter.getCurrentRPM());
        joinedTelemetry.addData("Target RPM", robot.outtake.shooter.getTargetRPM());
        joinedTelemetry.addData("Current Power", robot.outtake.shooter.getCurrentPower());
        joinedTelemetry.addData("Use Flywheel", robot.outtake.shooter.getUseFlywheel());
        joinedTelemetry.update();
    }

    // Gamepad tuning, writes straight to Shooter's static fields so it doesn't depend on Panels.
    // dpad left/right: pick a value, dpad up/down: change it, bumpers: step x10 / /10
    private void gamepadTune(){
        if (!previousGamepad1.dpad_right && currentGamepad1.dpad_right){
            selected = (selected + 1) % PARAM_NAMES.length;
        }
        if (!previousGamepad1.dpad_left && currentGamepad1.dpad_left){
            selected = (selected + PARAM_NAMES.length - 1) % PARAM_NAMES.length;
        }
        if (!previousGamepad1.right_bumper && currentGamepad1.right_bumper){
            steps[selected] *= 10;
        }
        if (!previousGamepad1.left_bumper && currentGamepad1.left_bumper){
            steps[selected] /= 10;
        }

        double change = 0;
        if (!previousGamepad1.dpad_up && currentGamepad1.dpad_up) change = steps[selected];
        if (!previousGamepad1.dpad_down && currentGamepad1.dpad_down) change = -steps[selected];
        if (change == 0) return;

        switch (selected) {
            case 0: Shooter.flywheelTargetRPM = Math.max(0, Shooter.flywheelTargetRPM + change); break;
            case 1: Shooter.p = Math.max(0, Shooter.p + change); break;
            case 2: Shooter.i = Math.max(0, Shooter.i + change); break;
            case 3: Shooter.d = Math.max(0, Shooter.d + change); break;
            case 4: Shooter.f = Math.max(0, Shooter.f + change); break;
        }
    }

    public void gamepadUpdate(){
        previousGamepad1.copy(currentGamepad1);
        currentGamepad1.copy(gamepad1);

        previousGamepad2.copy(currentGamepad2);
        currentGamepad2.copy(gamepad2);
    }
}
