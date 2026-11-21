package org.firstinspires.ftc.teamcode.subsystems.intake;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;

@Configurable
public class Intake implements Subsystem {

    //---------------- Subsystems ----------------
    public Ramp ramp;
    public Spinner spinner;
    public Transfer transfer;

    public enum Mode { STOPPED, INTAKING, FULL, OUTTAKING, FIRING }

    private static final double HOLD_POWER = 0.3;
    public static double shotFeedPower = 0.5;
    // Diagnostics can command the motors directly until a control or action requests a mode.
    private Mode requestedMode;
    private Mode mode = Mode.STOPPED;

    //---------------- Constructor ----------------
    public Intake(HardwareMap map) {
        ramp = new Ramp(map);
        spinner = new Spinner(map);
        transfer = new Transfer(map);
    }

    //---------------- Methods ----------------
    public void intakeIn(){
        spinner.setSpinnerSpinIn();
        transfer.setTransferIn();
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

    public void collect() { requestedMode = Mode.INTAKING; }
    public void eject() { requestedMode = Mode.OUTTAKING; }
    public void stopCollecting() { requestedMode = Mode.STOPPED; }
    public Mode getMode() { return mode; }

    /** Resolve collection, driver eject override, and the outtake's feed request in one place. */
    public void updateCommands(boolean firing) {
        if (requestedMode == Mode.OUTTAKING) {
            mode = Mode.OUTTAKING;
            intakeOut();
        } else if (firing) {
            mode = Mode.FIRING;
            spinner.setSpinnerSpinIn(Range.clip(shotFeedPower, 0.0, 1.0));
            transfer.setTransferIn(1.0);
        } else if (requestedMode == Mode.INTAKING) {
            if (spinner.isFull()) {
                mode = Mode.FULL;
                spinner.setSpinnerSpinIn(HOLD_POWER);
                transfer.setTransferIn(0.2);
            } else {
                mode = Mode.INTAKING;
                spinner.setSpinnerSpinIn();
                if (spinner.getBallCount() >= 1) transfer.setTransferIn(0.2);
                else transfer.setTransferIn();
            }
        } else if (requestedMode == Mode.STOPPED || mode == Mode.FIRING) {
            mode = Mode.STOPPED;
            intakeStop();
        } else {
            return;
        }

        boolean open = mode == Mode.FIRING;
        if (open != transfer.isBlockerOpen()) {
            if (open) transfer.openBlocker();
            else transfer.closeBlocker();
        }
    }

    /** Stop the feed motors and close the blocker even if the OpMode never updates again. */
    public void stopFeed() {
        mode = Mode.STOPPED;
        try {
            spinner.stop();
        } finally {
            transfer.stop();
        }
    }

    //---------------- Interface Methods ----------------
    @Override
    public void init(){
        requestedMode = null;
        mode = Mode.STOPPED;
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
        requestedMode = null;
        try {
            stopFeed();
        } finally {
            ramp.stop();
        }
    }
}
