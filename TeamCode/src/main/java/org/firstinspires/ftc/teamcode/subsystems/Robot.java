package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.pedropathing.follower.Follower;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.pedro.FollowerManager;
import org.firstinspires.ftc.teamcode.subsystems.other.Drive;
import org.firstinspires.ftc.teamcode.subsystems.other.Drive2_0;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Outtake;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.vision.Vision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Robot {

    //---------------- Objects ----------------
    private final HardwareMap hardwareMap;
    private final Telemetry telemetry;
    private Gamepad gp1;
    private Gamepad gp2;
    private static VoltageSensor voltageSensor;

    //---------------- Subsystems ----------------
    public Intake intake;
    public Outtake outtake;
    public Vision vision;
    public Drive drive;
    public Drive2_0 drive20;
    private List<Subsystem> subsystems;

    //---------------- Constructors ----------------
    public Robot(HardwareMap hardwareMap, Telemetry telemetry, Gamepad gp1, Gamepad gp2) {
        this(hardwareMap, telemetry, gp1, gp2, null);
    }

    private Robot(HardwareMap hardwareMap, Telemetry telemetry, Gamepad gp1, Gamepad gp2,
                  Follower follower) {
        this.hardwareMap = hardwareMap;
        this.telemetry = telemetry;

        intake = new Intake(hardwareMap);
        outtake = new Outtake(hardwareMap);
        vision = Vision.getInstance(hardwareMap);
        if (follower == null) {
            // Legacy diagnostics use direct motor control, never a second Pedro follower.
            FollowerManager.clearHandoff();
            drive = new Drive(hardwareMap);
        } else {
            drive20 = new Drive2_0(follower);
        }



        subsystems = new ArrayList<>(Arrays.asList(intake, outtake, vision));
        if (drive != null) subsystems.add(drive);
        // Drive2_0 only sends commands; FollowerManager owns its update/stop lifecycle.

        this.gp1 = gp1;
        this.gp2 = gp2;

        voltageSensor = hardwareMap.voltageSensor.iterator().next();
    }

    public Robot(HardwareMap hardwareMap, Telemetry telemetry) {
        this(hardwareMap, telemetry, null, null);
    }

    /** Auto and Pedro-based TeleOp receive the same follower owned by their OpMode manager. */
    public Robot(HardwareMap hardwareMap, Telemetry telemetry, Follower follower) {
        this(hardwareMap, telemetry, null, null, java.util.Objects.requireNonNull(follower));
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
