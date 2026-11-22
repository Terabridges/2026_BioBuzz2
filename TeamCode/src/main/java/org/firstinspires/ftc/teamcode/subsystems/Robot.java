package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Outtake;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.vision.Vision;

import java.util.Arrays;
import java.util.List;

/** Mechanism hardware and lifecycle. The OpMode owns its drivetrain separately. */
public class Robot {

    //---------------- Subsystems ----------------
    public Intake intake;
    public Outtake outtake;
    public Vision vision;
    private final List<Subsystem> subsystems;

    //---------------- Constructors ----------------
    public Robot(HardwareMap hardwareMap, Telemetry telemetry) {
        intake = new Intake(hardwareMap);
        outtake = new Outtake(hardwareMap);
        vision = Vision.getInstance(hardwareMap);
        subsystems = Arrays.asList(intake, outtake, vision);
    }

    //---------------- Interface Methods ----------------
    public void toInit() {
        for (Subsystem s : subsystems) {
            s.init();
        }
    }

    public void update() {
        outtake.updateShooting(intake.spinner.getBallCount());
        intake.updateCommands(outtake.isFiring(), outtake.shouldRunFeedMotors());
        for (Subsystem s : subsystems) {
            s.update();
        }
    }

    public void stop() {
        RuntimeException failure = null;
        for (Subsystem s : subsystems) {
            try {
                s.stop();
            } catch (RuntimeException stopFailure) {
                if (failure == null) failure = stopFailure;
                else if (failure != stopFailure) failure.addSuppressed(stopFailure);
            }
        }
        if (failure != null) throw failure;
    }

    public void logPsiKitData() {
        for (Subsystem s : subsystems) {
            s.logPsiKitData();
        }
    }

}
