package org.firstinspires.ftc.teamcode.subsystems.intake;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.psilynx.psikit.core.Logger;

public class Intake implements Subsystem {
    //---------------- Hardware -----------------
    public Spinner spinner;

    //---------------- Software ----------------
    public boolean autoIntake;
    public boolean useSortingIntake = true;


    //---------------- Constructor ----------------
    public Intake(HardwareMap map) {
        spinner = new Spinner(map);
    }

    //---------------- Methods ----------------
    public void toggleAutoIntake(){
        spinner.autoSpin = !spinner.autoSpin;
        autoIntake = spinner.autoSpin;
    }

    public void toggleAutoIntake2(){
        autoIntake = !autoIntake;
    }


    public double getFloodgateCurrentAmps() {
        return spinner.getFloodgateCurrentAmps();
    }

    //---------------- Interface Methods ----------------
    @Override
    public void toInit(){
        spinner.toInit();
    }

    @Override
    public void update(){
        spinner.update();
    }

    @Override
    public void stop(){}

    @Override
    public void logPsiKitData() {
        Logger.recordOutput("Subsystems/Intake/SpinnerAutoSpin", spinner.autoSpin);

        spinner.logPsiKitData();
    }
}
