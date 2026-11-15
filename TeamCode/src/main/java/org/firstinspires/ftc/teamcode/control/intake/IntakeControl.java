package org.firstinspires.ftc.teamcode.control.intake;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.intake.Ramp;
import org.firstinspires.ftc.teamcode.subsystems.intake.Spinner;
import org.firstinspires.ftc.teamcode.subsystems.intake.Transfer;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;

import java.util.function.BooleanSupplier;

/**
 * Gamepad 1: A toggles the ramp, RT intakes, LT outtakes. This class also owns the blocker and the feed
 * motors, so when the shooter wants to fire (see OuttakeControl) the balls are fed from here.
 */
public class IntakeControl implements Control {

    private enum State {
        STOPPED,
        INTAKING,
        FULL,       // RT is held with 4 balls already in: spinner and transfer at low power
        OUTTAKING,
        FIRING
    }

    //---------------- Software ----------------
    private final Intake intake;
    private final Ramp ramp;
    private final Spinner spinner;
    private final Transfer transfer;
    private final Gamepad gp1;
    private final Gamepad gp2;
    private final BooleanSupplier firing; // true while the shooter wants the balls fed to it

    private final double TRIGGER_THRESHOLD = 0.1;
    // Low power that holds balls in place: the transfer uses it once a ball is waiting at the blocker,
    // and the spinner uses it too once the robot is full
    private final double HOLD_POW = 0.3;
    private final int MAX_BALLS = 4; // a robot may only control 4 at once

    private State state = State.STOPPED;

    private final EdgeDetector toggleRampRE;

    //---------------- Constructors ----------------
    // firing: asked every loop. While it's true the blocker opens and everything feeds toward the flywheel
    public IntakeControl(Intake intake, Gamepad gp1, Gamepad gp2, BooleanSupplier firing){
        this.intake = intake;
        this.ramp = intake.ramp;
        this.spinner = intake.spinner;
        this.transfer = intake.transfer;
        this.gp1 = gp1;
        this.gp2 = gp2;
        this.firing = firing;
        this.toggleRampRE = new EdgeDetector(() -> ramp.toggleRamp());
    }

    // Intake only, never fires (for tests without a shooter)
    public IntakeControl(Intake intake, Gamepad gp1, Gamepad gp2){
        this(intake, gp1, gp2, () -> false);
    }

    //---------------- Helper Methods ----------------
    // Only tells the blocker to move when it needs to change, so the servo isn't rewritten every loop
    private void setBlockerOpen(boolean open){
        if (open == transfer.isBlockerOpen()) return;
        if (open) transfer.openBlocker();
        else transfer.closeBlocker();
    }

    //---------------- Control Sub-methods ----------------
    private void updateRamp(){
        toggleRampRE.update(gp1.a);
    }

    private void updateIntake(){
        boolean in = gp1.right_trigger > TRIGGER_THRESHOLD;
        boolean out = gp1.left_trigger > TRIGGER_THRESHOLD;

        if (out && !in) {
            // Outtake always works, even when full or in the middle of a shot: both motors full power backward
            state = State.OUTTAKING;
            setBlockerOpen(false);
            intake.intakeOut();
        } else if (firing.getAsBoolean()) {
            // Dump everything into the flywheel
            state = State.FIRING;
            setBlockerOpen(true);
            intake.intakeIn();
        } else if (in && !out && spinner.getBallCount() >= MAX_BALLS) {
            // Full: the spinner drops to the same low power as the transfer, so the balls are held in
            // place without being pushed hard against the blocker
            state = State.FULL;
            setBlockerOpen(false);
            intake.intakeIn(HOLD_POW);
        } else if (in && !out) {
            // Full power until the first ball reaches the last sensor (it's now waiting at the blocker),
            // then the transfer drops so it only holds that ball in place while more come in behind it
            state = State.INTAKING;
            setBlockerOpen(false);
            spinner.setSpinnerSpinIn();
            if (spinner.isSensor4Broken()) transfer.setTransferIn(HOLD_POW);
            else transfer.setTransferIn();
        } else {
            state = State.STOPPED;
            setBlockerOpen(false);
            intake.intakeStop();
        }
    }

    //---------------- Interface Methods ----------------
    @Override
    public void update(){
        updateRamp();
        updateIntake();
    }

    @Override
    public void addTelemetry(Telemetry telemetry){
        telemetry.addData("Intake", state);
        telemetry.addData("Ball Count", spinner.getBallCount());
        telemetry.addData("Sensor 4 Broken", spinner.isSensor4Broken());
        telemetry.addData("Ramp Up", ramp.isRampUp());
        telemetry.addData("Blocker Open", transfer.isBlockerOpen());
    }
}
