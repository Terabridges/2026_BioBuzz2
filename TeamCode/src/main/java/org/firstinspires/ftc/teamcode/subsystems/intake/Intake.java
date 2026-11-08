package org.firstinspires.ftc.teamcode.subsystems.intake;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Shooter;
import org.psilynx.psikit.core.Logger;

public class Intake implements Subsystem {

    //---------------- Subsystems ----------------
    public Ramp ramp;
    public Spinner spinner;
    public Transfer transfer;

    //---------------- Software ----------------


    //---------------- Constructor ----------------
    public Intake(HardwareMap map) {
        ramp = new Ramp(map);
        spinner = new Spinner(map);
        transfer = new Transfer(map);
    }

    //---------------- Methods ----------------
    public void intakeIn(){
        if (spinner.getBallCount() == 0) {
            spinner.setSpinnerSpinIn(1.0);
            transfer.setTransferIn(0.3);
        } else if (spinner.getBallCount() == 1) {
            spinner.setSpinnerSpinIn(1.0);
            if (spinner.isSensor4Broken()) {
                transfer.setTransferZero();
            } else {
                transfer.setTransferIn(0.3);
            }
        } else if (spinner.getBallCount() == 2 || spinner.getBallCount() == 3) {
            spinner.setSpinnerSpinIn(1.0);
            transfer.setTransferZero();
        } else if (spinner.isFull()) {
            transfer.setTransferZero();
            spinner.setSpinnerSpinZero();
        }

        if (transfer.isBlockerOpen()) {
            spinner.setSpinnerSpinIn();
            transfer.setTransferIn();
        }
    }

    public void intakeOut(){
        spinner.setSpinnerSpinOut();
        transfer.setTransferOut();
    }

    public void intakeIn(double pow){
        spinner.setSpinnerSpinIn(pow);
        transfer.setTransferIn(pow);
    }

    public void intakeOut(double pow){
        spinner.setSpinnerSpinOut(pow);
        transfer.setTransferOut(pow);
    }

    public void intakeStop(){
        spinner.setSpinnerSpinZero();
        transfer.setTransferZero();
    }

    //---------------- Interface Methods ----------------
    @Override
    public void init(){
        ramp.init();
        spinner.init();
        transfer.init();
    }

    @Override
    public void update(){
        ramp.update();
        spinner.update();
        transfer.update();
    }

    @Override
    public void stop(){
        try {
            spinner.stop();
        } finally {
            transfer.stop();
        }
        ramp.stop();
    }
}
