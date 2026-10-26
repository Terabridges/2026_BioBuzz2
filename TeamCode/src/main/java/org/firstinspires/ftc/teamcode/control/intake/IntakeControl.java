package org.firstinspires.ftc.teamcode.control.intake;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.intake.Ramp;
import org.firstinspires.ftc.teamcode.subsystems.intake.Spinner;
import org.firstinspires.ftc.teamcode.subsystems.intake.Transfer;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;

public class IntakeControl implements Control {

    //---------------- Software ----------------
    Intake intake;
    Ramp ramp;
    Spinner spinner;
    Transfer transfer;
    Gamepad gp1;
    Gamepad gp2;

    private final double TRIGGER_THRESHOLD = 0.1;
    private final double INTAKE_MIN_POW = 0.5;
    private final double INTAKE_MAX_POW = 0.98;

    EdgeDetector toggleRampRE = new EdgeDetector(() -> ramp.toggleRamp());
    EdgeDetector toggleBlockerRE = new EdgeDetector(() -> transfer.toggleBlocker());

    // 1 = in, -1 = out, 0 = stopped
    private int intakeDir = 0;

    //---------------- Constructor ----------------
    public IntakeControl(Intake intake, Gamepad gp1, Gamepad gp2){
        this.intake = intake;
        this.ramp = intake.ramp;
        this.spinner = intake.spinner;
        this.transfer = intake.transfer;
        this.gp1 = gp1;
        this.gp2 = gp2;
    }

    //---------------- Methods ----------------
    private void updateRamp(){
        toggleRampRE.update(gp1.a);
    }

    // Trigger past the threshold maps to power: below 0.5 -> 0.5, 0.5-0.98 -> same, above 0.98 -> 0.98
    private double triggerToIntakePow(double trigger){
        return Math.min(Math.max(trigger, INTAKE_MIN_POW), INTAKE_MAX_POW);
    }

    // Spinner + transfer: RTrigger held = in, LTrigger held = out, both or neither = stop
    private void updateIntake(){
        boolean in = gp1.right_trigger > TRIGGER_THRESHOLD;
        boolean out = gp1.left_trigger > TRIGGER_THRESHOLD;
        int dir = (in == out) ? 0 : (in ? 1 : -1);

        // Power follows the trigger every loop while held, stop is only sent once on release
        if (dir == 1) intake.intakeIn(triggerToIntakePow(gp1.right_trigger));
        else if (dir == -1) intake.intakeOut(triggerToIntakePow(gp1.left_trigger));
        else if (intakeDir != 0) intake.intakeStop();
        intakeDir = dir;
    }

    private void updateBlocker(){
        toggleBlockerRE.update(gp1.y);
    }

    //---------------- Interface Methods ----------------
    @Override
    public void update(){
        updateRamp();
        updateIntake();
        updateBlocker();
    }

    @Override
    public void addTelemetry(Telemetry telemetry){
        telemetry.addData("Ramp Up", ramp.isRampUp());
        telemetry.addData("Blocker Open", transfer.isBlockerOpen());
        telemetry.addData("Intake Dir", intakeDir);
    }
}
