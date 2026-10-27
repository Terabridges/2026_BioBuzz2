package org.firstinspires.ftc.teamcode.control.intake;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.intake.Ramp;
import org.firstinspires.ftc.teamcode.subsystems.intake.Spinner;
import org.firstinspires.ftc.teamcode.subsystems.intake.Transfer;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;

public class IntakeControl implements Control {

    //---------------- Mode & State Definitions ----------------
    public enum ShootingMode {
        MANUAL_TRIGGERS, // Standard trigger-based intake control
        SINGLE_SHOT      // Automated single-ball shoot sequence using beam breaker
    }

    private enum SingleShotState {
        IDLE,
        FEEDING,
        BALL_DETECTED,
        REJECT_TRAILING
    }

    //---------------- Software (Original Variables Preserved) ----------------
    Intake intake;
    Ramp ramp;
    Spinner spinner;
    Transfer transfer;
    Gamepad gp1;
    Gamepad gp2;

    EdgeDetector spinInRE = new EdgeDetector(() -> spinner.setSpinnerSpinIn());
    EdgeDetector spinInFE = new EdgeDetector(() -> spinner.setSpinnerSpinZero(), true);
    EdgeDetector spinOutRE = new EdgeDetector(() -> spinner.setSpinnerSpinOut());
    EdgeDetector spinOutFE = new EdgeDetector(() -> spinner.setSpinnerSpinZero(), true);

    //---------------- Additional Software ----------------
    private ShootingMode currentMode = ShootingMode.MANUAL_TRIGGERS;
    private SingleShotState singleShotState = SingleShotState.IDLE;

    private boolean lastSensor4State = false;
    private ElapsedTime reverseTimer = new ElapsedTime();

    // Controls for Mode Toggle and Single Shot Trigger
    private EdgeDetector modeSwitchBtn = new EdgeDetector(() -> toggleShootingMode());
    private EdgeDetector singleShotBtn = new EdgeDetector(() -> triggerSingleShot());

    //---------------- Constructor ----------------
    public IntakeControl(Intake intake, Gamepad gp1, Gamepad gp2){
        this.intake = intake;
        this.ramp = intake.ramp;
        this.spinner = intake.spinner;
        this.transfer = intake.transfer;
        this.gp1 = gp1;
        this.gp2 = gp2;
    }

    //---------------- Helper Methods ----------------
    private void toggleShootingMode() {
        if (currentMode == ShootingMode.MANUAL_TRIGGERS) {
            currentMode = ShootingMode.SINGLE_SHOT;
        } else {
            currentMode = ShootingMode.MANUAL_TRIGGERS;
        }
        resetStates();
    }

    private void triggerSingleShot() {
        if (currentMode == ShootingMode.SINGLE_SHOT && singleShotState == SingleShotState.IDLE) {
            transfer.openBlocker();
            spinner.setSpinnerSpinIn();
            singleShotState = SingleShotState.FEEDING;
        }
    }

    private void resetStates() {
        singleShotState = SingleShotState.IDLE;
        spinner.setSpinnerSpinZero();
        transfer.closeBlocker();
    }

    //---------------- Control Methods ----------------
    private void updateRamp(){

    }

    private void updateSpinner(){
        // Mode Switch (gp1.y) & Single Shot Execution (gp1.a)
        modeSwitchBtn.update(gp1.y);
        singleShotBtn.update(gp1.a);

        if (currentMode == ShootingMode.MANUAL_TRIGGERS) {
            // Original Trigger Control Logic
            spinInRE.update(gp1.right_trigger > 0.1);
            spinInFE.update(gp1.right_trigger > 0.1);
            spinOutRE.update(gp1.left_trigger > 0.1);
            spinOutFE.update(gp1.left_trigger > 0.1);
        } else {
            // Beam-Breaker Single-Shot Automation Logic
            boolean currentSensor4 = spinner.isSensor4Broken();

            switch (singleShotState) {
                case IDLE:
                    break;

                case FEEDING:
                    if (currentSensor4) {
                        singleShotState = SingleShotState.BALL_DETECTED;
                    }
                    break;

                case BALL_DETECTED:
                    // Falling Edge: Ball was breaking sensor 4, now clear
                    if (!currentSensor4 && lastSensor4State) {
                        transfer.closeBlocker();
                        spinner.setSpinnerPow(-0.3); // Quick reverse tap to back off trailing ball
                        reverseTimer.reset();
                        singleShotState = SingleShotState.REJECT_TRAILING;
                    }
                    break;

                case REJECT_TRAILING:
                    if (reverseTimer.milliseconds() > 80) {
                        spinner.setSpinnerSpinZero();
                        singleShotState = SingleShotState.IDLE;
                    }
                    break;
            }

            lastSensor4State = currentSensor4;
        }
    }

    private void updateTransfer(){

    }

    //---------------- Interface Methods ----------------
    @Override
    public void update(){
        updateRamp();
        updateSpinner();
        updateTransfer();
    }

    @Override
    public void addTelemetry(Telemetry telemetry){
        telemetry.addData("Shooting Mode", currentMode);
        telemetry.addData("Single Shot State", singleShotState);
        telemetry.addData("Ball Count", spinner.getBallCount());
        telemetry.addData("Sensor 4 Broken", spinner.isSensor4Broken());
    }
}