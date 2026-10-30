package org.firstinspires.ftc.teamcode.control.outtake;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Lights;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Outtake;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Shooter;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Turret;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.psilynx.psikit.core.Logger;


public class OuttakeControl implements Control {

    //---------------- Software ----------------
    private final Shooter shooter;
    private final Turret turret;
    private final Lights lights;

    private final List<Supplier<Boolean>> suppliers;
    private final List<Consumer<Boolean>> consumers;

    //---------------- Constructor ----------------
    public OuttakeControl(Outtake outtake, Gamepad gp1, Gamepad gp2){
        this.shooter = outtake.shooter;
        this.turret = outtake.turret;
        this.lights = outtake.lights;

        this.suppliers = new ArrayList<>();
        this.consumers = new ArrayList<>();

        connect(() -> gp1.x, shooter::toggleShooting);
        connect(() -> gp1.dpad_right, shooter::increaseShootRPM);
        connect(() -> gp1.dpad_left, shooter::decreaseShootRPM);
        connect(() -> gp1.dpad_up, shooter::hoodUp);
        connect(() -> gp1.dpad_down, shooter::hoodDown);

        // LBump held = left, RBump held = right, both or neither = stop
        connect(() -> gp1.left_bumper && !gp1.right_bumper, turret::turnTurretLeft);
        connect(() -> !gp1.left_bumper && gp1.right_bumper, turret::turnTurretRight);
        connect(() -> gp1.left_bumper == gp1.right_bumper, turret::stopTurret);
    }

    private void connect(Supplier<Boolean> someGamePadControl, Runnable thingToDo) {
        suppliers.add(someGamePadControl);
        consumers.add(new EdgeDetector(thingToDo));
    }

    //---------------- Methods ----------------
    private void updateShooter() {
        for (int i = 0; i < suppliers.size(); i++) {
            Supplier<Boolean> oneSupplier = suppliers.get(i);
            Consumer<Boolean> oneConsumer = consumers.get(i);
            oneConsumer.accept(oneSupplier.get());
        }
    }

    private void updateLights(){

    }

    //---------------- Interface Methods ----------------
    @Override
    public void update() {
        updateShooter();
        updateLights();
    }

    @Override
    public void addTelemetry(Telemetry telemetry) {
        telemetry.addData("Shooting", shooter.isShooting());
        telemetry.addData("Shoot RPM", shooter.getShootRPM());
        telemetry.addData("Shooter Target RPM", shooter.getTargetRPM());
        telemetry.addData("Shooter Current RPM", shooter.getCurrentRPM());
        telemetry.addData("Hood Pos", shooter.getHoodPos());
        telemetry.addData("Turret Pos", turret.getTurretPos());
        Logger.recordOutput("Shooter Target RPM", shooter.getTargetRPM());
        Logger.recordOutput("Shooter Current RPM", shooter.getCurrentRPM());
        Logger.recordOutput("Raw Flywheel Vel", shooter.getFlywheelVel());
    }
}
