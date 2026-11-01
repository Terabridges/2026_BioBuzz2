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
        for (Subsystem s : subsystems) {
            s.update();
        }
    }

    public void stop() {
        for (Subsystem s : subsystems) {
            s.stop();
        }
    }

    public void logPsiKitData() {
        for (Subsystem s : subsystems) {
            s.logPsiKitData();
        }
    }

}
