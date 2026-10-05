package org.firstinspires.ftc.teamcode.subsystems.outtake;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;

public class Outtake implements Subsystem {

    //---------------- Subsystems ----------------
    private Shooter shooter;
    private Turret turret;


    //---------------- Software ----------------


    //---------------- Constructor ----------------
    public Outtake(HardwareMap map) {
        shooter = new Shooter(map);
        turret = new Turret(map);
    }

    //---------------- Methods ----------------


    //---------------- Interface Methods ----------------
    @Override
    public void toInit(){
        shooter.toInit();
        turret.toInit();
    }

    @Override
    public void update(){
        shooter.update();
        turret.update();
    }

    @Override
    public void stop(){}
}
