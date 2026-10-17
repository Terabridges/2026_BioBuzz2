package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.VoltageSensor;

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
    FollowerManager followerManager;
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
        this.hardwareMap = hardwareMap;
        this.telemetry = telemetry;

        intake = new Intake(hardwareMap);
        outtake = new Outtake(hardwareMap);
        vision = Vision.getInstance(hardwareMap);
        drive = new Drive(hardwareMap);
        drive20 = new Drive2_0(hardwareMap);



        subsystems = new ArrayList<>(Arrays.asList(intake, outtake, vision, drive20));

        this.gp1 = gp1;
        this.gp2 = gp2;

        voltageSensor = hardwareMap.voltageSensor.iterator().next();
    }

    public Robot(HardwareMap hardwareMap, Telemetry telemetry) {
        this(hardwareMap, telemetry, null, null);
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
