package org.firstinspires.ftc.teamcode.control.outtake;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Lights;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Outtake;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Shooter;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Turret;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;

public class OuttakeControl implements Control {

    //---------------- Software ----------------
    Outtake outtake;
    Shooter shooter;
    Turret turret;
    Lights lights;
    Gamepad gp1;
    Gamepad gp2;

    private final double SHOOT_RPM = 2000;
    private EdgeDetector toggleShooterEdgeDetect = new EdgeDetector(this::toggleShooter);

    //---------------- Constructor ----------------
    public OuttakeControl(Outtake outtake, Gamepad gp1, Gamepad gp2){
        this.outtake = outtake;
        this.shooter = outtake.shooter;
        this.turret = outtake.turret;
        this.lights = outtake.lights;
        this.gp1 = gp1;
        this.gp2 = gp2;
    }

    //---------------- Methods ----------------
    public void toggleShooter() {
        shooter.setTargetRPM(Shooter.flywheelTargetRPM > 0 ? 0 : SHOOT_RPM);
    }

    private void updateShooter(){
        toggleShooterEdgeDetect.update(gp1.y);
    }

    private void updateTurret(){

    }

    private void updateLights(){

    }

    //---------------- Interface Methods ----------------
    @Override
    public void update() {
        updateShooter();
        updateTurret();
        updateLights();
    }

    @Override
    public void addTelemetry(Telemetry telemetry) {
        telemetry.addData("Shooter Target RPM", shooter.getTargetRPM());
        telemetry.addData("Shooter Current RPM", shooter.getCurrentRPM());
        telemetry.addData("Shooter Flywheel Power", 2000 * shooter.getCurrentPower());
    }
}