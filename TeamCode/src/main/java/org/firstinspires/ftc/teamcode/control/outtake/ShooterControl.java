package org.firstinspires.ftc.teamcode.control.outtake;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Outtake;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Shooter;
import org.firstinspires.ftc.teamcode.util.EdgeDetector;

public class ShooterControl implements Control {

    //---------------- Software ----------------
    Shooter shooter;
    Gamepad gp1;
    Gamepad gp2;
    Robot robot;

    private final double SHOOT_RPM = 2000;
    private EdgeDetector toggleShooterEdgeDetect = new EdgeDetector(this::toggleShooter);

    //---------------- Constructor ----------------
    public ShooterControl(Outtake outtake, Gamepad gp1, Gamepad gp2){
        this.shooter = outtake.shooter;
        this.gp1 = gp1;
        this.gp2 = gp2;
    }

    public ShooterControl(Robot robot, Gamepad gp1, Gamepad gp2) {
        this.robot = robot;
        this.shooter = robot.outtake.shooter;
        this.gp1 = gp1;
        this.gp2 = gp2;
    }

    //---------------- Methods ----------------
    public void toggleShooter() {
        shooter.setTargetRPM(Shooter.flywheelTargetRPM > 0 ? 0 : SHOOT_RPM);
    }

    //---------------- Interface Methods ----------------
    @Override
    public void update(){
        toggleShooterEdgeDetect.update(gp1.y);
    }

    @Override
    public void addTelemetry(Telemetry telemetry){
        telemetry.addData("Shooter Target RPM", shooter.getTargetRPM());
        telemetry.addData("Shooter Current RPM", shooter.getCurrentRPM());
    }
}
