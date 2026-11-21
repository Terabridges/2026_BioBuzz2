package org.firstinspires.ftc.teamcode.control.intake;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;

/**
 * Gamepad 1: A toggles the ramp, RT collects, LT ejects.
 */
public class IntakeControl implements Control {

    private static final double TRIGGER_THRESHOLD = 0.1;
    private final Intake intake;
    private final Gamepad gp1;
    private final EdgeDetector toggleRampRE;

    public IntakeControl(Intake intake, Gamepad gp1){
        this.intake = intake;
        this.gp1 = gp1;
        this.toggleRampRE = new EdgeDetector(intake.ramp::toggleRamp);
    }

    @Override
    public void update(){
        toggleRampRE.update(gp1.a);
        boolean in = gp1.right_trigger > TRIGGER_THRESHOLD;
        boolean out = gp1.left_trigger > TRIGGER_THRESHOLD;
        if (out && !in) intake.eject();
        else if (in && !out) intake.collect();
        else intake.stopCollecting();
    }

    @Override
    public void addTelemetry(Telemetry telemetry){
        telemetry.addData("Intake", intake.getMode());
        telemetry.addData("Ball Count", intake.spinner.getBallCount());
        telemetry.addData("Sensor 4 Broken", intake.spinner.isSensor4Broken());
        telemetry.addData("Ramp Up", intake.ramp.isRampUp());
        telemetry.addData("Blocker Open", intake.transfer.isBlockerOpen());
    }
}
