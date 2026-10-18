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

    private EdgeDetector toggleShooterEdgeDetect = new EdgeDetector(() -> shooter.toggleShooting());
    private EdgeDetector increaseRPMEdgeDetect = new EdgeDetector(() -> shooter.increaseShootRPM());
    private EdgeDetector decreaseRPMEdgeDetect = new EdgeDetector(() -> shooter.decreaseShootRPM());
    private EdgeDetector hoodUpEdgeDetect = new EdgeDetector(() -> shooter.hoodUp());
    private EdgeDetector hoodDownEdgeDetect = new EdgeDetector(() -> shooter.hoodDown());

    // 1 = left (counterclockwise), -1 = right (clockwise), 0 = stopped. Only sent to the turret when it changes
    private int turretDir = 0;

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
    private void updateShooter(){
        toggleShooterEdgeDetect.update(gp1.x);
        increaseRPMEdgeDetect.update(gp1.dpad_right);
        decreaseRPMEdgeDetect.update(gp1.dpad_left);
        hoodUpEdgeDetect.update(gp1.dpad_up);
        hoodDownEdgeDetect.update(gp1.dpad_down);
    }

    // LBump held = left, RBump held = right, both or neither = stop
    private void updateTurret(){
        boolean left = gp1.left_bumper;
        boolean right = gp1.right_bumper;
        int dir = (left == right) ? 0 : (left ? 1 : -1);

        if (dir != turretDir){
            if (dir == 1) turret.turnTurretLeft();
            else if (dir == -1) turret.turnTurretRight();
            else turret.stopTurret();
            turretDir = dir;
        }
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
        telemetry.addData("Shooting", shooter.isShooting());
        telemetry.addData("Shoot RPM", shooter.getShootRPM());
        telemetry.addData("Shooter Target RPM", shooter.getTargetRPM());
        telemetry.addData("Shooter Current RPM", shooter.getCurrentRPM());
        telemetry.addData("Hood Pos", shooter.getHoodPos());
        telemetry.addData("Turret Pos", turret.getTurretPos());
    }
}
