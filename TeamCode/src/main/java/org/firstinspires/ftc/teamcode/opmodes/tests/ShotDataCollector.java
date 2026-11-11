package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.bylazar.configurables.annotations.Configurable;
import com.bylazar.telemetry.JoinedTelemetry;
import com.bylazar.telemetry.PanelsTelemetry;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
import org.firstinspires.ftc.teamcode.control.drive.DriveControl;
import org.firstinspires.ftc.teamcode.pedro.FollowerManager;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.subsystems.drive.PedroDrive;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Shooter;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Turret;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;
import org.firstinspires.ftc.teamcode.utility.shooterUtility.TurretAim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Collects ShooterData rows: drive to a distance, tune RPM and hood until single shots score, then save.
 * Saved rows show on telemetry as lines to paste into ShooterData, and go to a CSV on the Control Hub
 * (/sdcard/FIRST/shotData/), one file per run.
 *
 * <p>Distance comes from odometry (TurretAim: turret pivot → the Cell for the half of the field the robot is on).
 * Set manualDistance in Panels to a tape-measured value to use that instead; 0 goes back to odometry.
 *
 * <p>Init: X = blue, B = red (default red).
 * <p>Gamepad 1 after Start:
 * <pre>
 *   sticks            drive (Back = field-centric; no slow mode here, B is taken)
 *   B                 switch alliance (red / blue)
 *   A                 flywheel on/off
 *   d-pad right/left  RPM +/- rpmStep
 *   d-pad up/down     hood +/- hoodStep
 *   RB                fire one ball (only when the flywheel is at speed)
 *   RT / LT           intake in / out, to load balls
 *   X                 turret: locked forward / tracking the Cell
 *   Y                 save a row
 * </pre>
 * <p>Gamepad 2: LB deletes the last saved row (on its own controller so it can't be hit by accident).
 */
@Configurable
@TeleOp(name = "Shot Data Collector", group = "Test")
public class ShotDataCollector extends OpMode {

    //---------------- Tunable (Panels) ----------------
    public static double testRPM = 2000;
    public static double testHood = 0.5;
    public static double rpmStep = 10;
    public static double hoodStep = 0.01;
    public static double manualDistance = 0;    // inches; 0 = use odometry
    public static double readyTolerance = 50;   // RPM from target before a shot is allowed
    public static double feedTimeMs = 1000;      // how long one shot runs the feed. Tune so exactly one ball goes
    public static double feedPower = 1.0;

    // Start pose when autonomous didn't run first: robot in the audience-left corner, back against the
    // audience wall, left side against the left wall, facing away from the audience. The pose is the
    // robot's center, so half the width (385 mm) in x and half the length (416 mm) in y
    public static double startX = 385 / 2.0 / 25.4;   // 7.58 in.
    public static double startY = 416 / 2.0 / 25.4;   // 8.19 in.
    public static double startHeadingDeg = 90;

    //---------------- Objects ----------------
    private Robot robot;
    private Shooter shooter;
    private Turret turret;
    private Intake intake;
    private FollowerManager followerManager;
    private DriveControl driveControl;
    private TurretAim turretAim;
    private JoinedTelemetry joinedTelemetry;

    //---------------- Software ----------------
    private Alliance alliance = Alliance.RED;
    private boolean flywheelOn = false;
    private boolean tracking = false;
    private boolean feeding = false;
    private int shotCount = 0;
    private double distance = 0;
    private double appliedHood = Double.NaN;

    private final ElapsedTime feedTimer = new ElapsedTime();
    private final ElapsedTime telemetryTimer = new ElapsedTime();

    // Each row: distance, rpm, hood, x, y, heading (deg), manual distance (1) or odometry (0), blue (1) or red (0)
    private final List<double[]> rows = new ArrayList<>();
    private File csvFile;
    private String fileStatus = "nothing saved yet";

    private static final double TRIGGER_THRESHOLD = 0.1;

    //---------------- Buttons ----------------
    private final EdgeDetector toggleFlywheel = new EdgeDetector(() -> flywheelOn = !flywheelOn);
    private final EdgeDetector rpmUp = new EdgeDetector(() -> testRPM += rpmStep);
    private final EdgeDetector rpmDown = new EdgeDetector(() -> testRPM = Math.max(0, testRPM - rpmStep));
    private final EdgeDetector hoodUp = new EdgeDetector(() -> testHood += hoodStep);
    private final EdgeDetector hoodDown = new EdgeDetector(() -> testHood -= hoodStep);
    private final EdgeDetector fire = new EdgeDetector(this::fire);
    private final EdgeDetector toggleTracking = new EdgeDetector(() -> tracking = !tracking);
    private final EdgeDetector saveRow = new EdgeDetector(this::saveRow);
    private final EdgeDetector deleteRow = new EdgeDetector(this::deleteLastRow);
    private final EdgeDetector allianceButton = new EdgeDetector(this::switchAlliance);

    //---------------- init ----------------
    @Override
    public void init() {
        followerManager = FollowerManager.forTeleOp(hardwareMap);
        robot = new Robot(hardwareMap, telemetry);
        shooter = robot.outtake.shooter;
        turret = robot.outtake.turret;
        intake = robot.intake;
        turretAim = new TurretAim(alliance);

        joinedTelemetry = new JoinedTelemetry(PanelsTelemetry.INSTANCE.getFtcTelemetry(), telemetry);

        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        csvFile = new File(new File(AppUtil.FIRST_FOLDER, "shotData"), "shotData_" + stamp + ".csv");
    }

    @Override
    public void init_loop() {
        if (gamepad1.x) alliance = Alliance.BLUE;
        if (gamepad1.b) alliance = Alliance.RED;
        followerManager.update();

        telemetry.addData("Alliance (X = blue, B = red)", alliance);
        if (followerManager.resumedFromAuto()) {
            telemetry.addData("Localization", "From autonomous");
        } else {
            telemetry.addData("Localization", "Start pose (%.2f, %.2f) %.0f deg", startX, startY, startHeadingDeg);
            telemetry.addLine("Put the robot in the audience-left corner, facing away from the audience");
        }
        telemetry.addLine("Cell positions in TurretAim are placeholders until measured");
        telemetry.update();
    }

    //---------------- start ----------------
    @Override
    public void start() {
        turretAim.setAlliance(alliance);
        robot.toInit();
        // Set at Start rather than init, so a start pose changed in Panels during init still applies
        if (!followerManager.resumedFromAuto()) {
            followerManager.follower().setPose(new Pose(startX, startY, Math.toRadians(startHeadingDeg)));
        }
        followerManager.startManual();
        // Same drive settings as MainTeleOp: rates on, robot-centric to start. Made at Start like MainTeleOp,
        // because DriveControl reads its rate settings and the current button states when it's created.
        // B switches alliance in this OpMode, so its buttons are replaced: only Back (field-centric), no slow mode
        driveControl = new DriveControl(new PedroDrive(followerManager.follower()), gamepad1, true, false) {
            private final EdgeDetector fieldToggle = new EdgeDetector(this::toggleFieldCentric);
            { fieldToggle.wasPressed = gamepad.back; }

            @Override protected void updateButtons() {
                fieldToggle.update(gamepad.back);
            }
        };
        // B may still be held from picking red during init: don't let that count as a press
        allianceButton.wasPressed = gamepad1.b;
        appliedHood = Double.NaN; // write the hood on the first loop
        telemetryTimer.reset();
    }

    //---------------- loop ----------------
    @Override
    public void loop() {
        updateButtons();
        driveControl.update();

        Pose pose = followerManager.follower().pose();
        turretAim.update(pose);
        distance = manualDistance > 0 ? manualDistance : turretAim.getDistance();

        updateShooter();
        updateTurret();
        updateFeed();

        robot.update();
        followerManager.update();

        if (telemetryTimer.milliseconds() > 200) {
            addTelemetry(pose);
            telemetryTimer.reset();
        }
    }

    //---------------- stop ----------------
    @Override
    public void stop() {
        try {
            if (robot != null) robot.stop();
        } finally {
            if (followerManager != null) followerManager.stop();
        }
    }

    //---------------- Methods ----------------
    private void updateButtons() {
        toggleFlywheel.update(gamepad1.a);
        rpmUp.update(gamepad1.dpad_right);
        rpmDown.update(gamepad1.dpad_left);
        hoodUp.update(gamepad1.dpad_up);
        hoodDown.update(gamepad1.dpad_down);
        fire.update(gamepad1.right_bumper);
        toggleTracking.update(gamepad1.x);
        saveRow.update(gamepad1.y);
        deleteRow.update(gamepad2.left_bumper); // gamepad 2, so it's hard to hit by accident
        allianceButton.update(gamepad1.b);
    }

    // Aims at the other alliance's hive from now on. Rows already saved keep the alliance they were saved with
    private void switchAlliance() {
        alliance = alliance == Alliance.RED ? Alliance.BLUE : Alliance.RED;
        turretAim.setAlliance(alliance);
    }

    private void updateShooter() {
        shooter.setTargetRPM(flywheelOn ? testRPM : 0);

        // Only write the hood when the value changed (buttons or Panels). Shooter clamps it to its
        // limits, so read the clamped value back to keep testHood honest
        if (testHood != appliedHood) {
            shooter.setHoodPos(testHood);
            testHood = shooter.getHoodPos();
            appliedHood = testHood;
        }
    }

    private void updateTurret() {
        if (tracking) {
            turret.aimAt(turretAim.getTurretAngle());
        } else {
            turret.aimAt(0); // locked straight forward: aim by pointing the robot
        }
    }

    private boolean isReady() {
        return flywheelOn && Math.abs(shooter.getCurrentRPM() - testRPM) <= readyTolerance;
    }

    // One shot: open the blocker and run the feed for feedTimeMs, then close it again
    private void fire() {
        if (feeding || !isReady()) return;
        feeding = true;
        shotCount++;
        intake.transfer.openBlocker();
        intake.intakeIn(feedPower);
        feedTimer.reset();
    }

    private void updateFeed() {
        if (feeding) {
            if (feedTimer.milliseconds() >= feedTimeMs) {
                intake.intakeStop();
                intake.transfer.closeBlocker();
                feeding = false;
            }
            return; // the triggers don't touch the intake mid-shot
        }

        // Loading: RT in, LT out, both or neither stop. The blocker stays closed, so balls wait for the shooter
        boolean in = gamepad1.right_trigger > TRIGGER_THRESHOLD;
        boolean out = gamepad1.left_trigger > TRIGGER_THRESHOLD;
        if (in && !out) intake.intakeIn(triggerToPower(gamepad1.right_trigger));
        else if (out && !in) intake.intakeOut(triggerToPower(gamepad1.left_trigger));
        else intake.intakeStop();
    }

    // Same as MainTeleOp: a light press still gives 0.5, full press 0.98
    private double triggerToPower(double trigger) {
        return Math.min(Math.max(trigger, 0.5), 0.98);
    }

    private void saveRow() {
        Pose pose = followerManager.follower().pose();
        rows.add(new double[]{distance, testRPM, testHood, pose.x(), pose.y(), Math.toDegrees(pose.heading()),
                manualDistance > 0 ? 1 : 0, alliance == Alliance.BLUE ? 1 : 0});
        writeCsv();
    }

    private void deleteLastRow() {
        if (rows.isEmpty()) return;
        rows.remove(rows.size() - 1);
        writeCsv();
    }

    // Rewrites the whole file each time, so a deleted row also leaves the file
    private void writeCsv() {
        try {
            File folder = csvFile.getParentFile();
            if (folder != null && !folder.exists() && !folder.mkdirs()) {
                throw new IOException("can't create " + folder);
            }
            try (FileWriter writer = new FileWriter(csvFile, false)) {
                writer.write("distance_in,rpm,hood,x,y,heading_deg,alliance,distance_source\n");
                for (double[] r : rows) {
                    writer.write(String.format(Locale.US, "%.1f,%.0f,%.3f,%.1f,%.1f,%.1f,%s,%s%n",
                            r[0], r[1], r[2], r[3], r[4], r[5], r[7] == 1 ? Alliance.BLUE : Alliance.RED,
                            r[6] == 1 ? "manual" : "odometry"));
                }
            }
            fileStatus = rows.size() + " rows in " + csvFile.getName();
        } catch (IOException e) {
            fileStatus = "SAVE FAILED: " + e.getMessage();
        }
    }

    private void addTelemetry(Pose pose) {
        joinedTelemetry.addData("Alliance (B to switch)", alliance);
        joinedTelemetry.addData("Distance (in)", "%.1f  (%s)", distance,
                manualDistance > 0 ? "manual" : "odometry, " + turretAim.getCell() + " Cell");
        joinedTelemetry.addData("Target / current RPM", "%.0f / %.0f", testRPM, shooter.getCurrentRPM());
        joinedTelemetry.addData("Hood", "%.3f", testHood);
        joinedTelemetry.addData("Flywheel", flywheelOn ? (isReady() ? "READY" : "spinning up") : "off");
        joinedTelemetry.addData("Shots", shotCount);
        joinedTelemetry.addData("Turret", tracking
                ? String.format(Locale.US, "tracking, %.1f deg", Math.toDegrees(turretAim.getTurretAngle()))
                : "locked forward");
        joinedTelemetry.addData("Pose", "(%.1f, %.1f) %.1f deg", pose.x(), pose.y(), Math.toDegrees(pose.heading()));
        joinedTelemetry.addData("File", fileStatus);

        // Ready to paste into ShooterData, sorted by distance (InterpLUT needs increasing distances)
        List<double[]> sorted = new ArrayList<>(rows);
        sorted.sort((a, b) -> Double.compare(a[0], b[0]));
        joinedTelemetry.addLine("---- Saved rows (" + rows.size() + ") ----");
        for (double[] r : sorted) {
            joinedTelemetry.addLine(String.format(Locale.US, "rpmLUT.add(%.1f, %.0f);  angleLUT.add(%.1f, %.3f);",
                    r[0], r[1], r[0], r[2]));
        }
        joinedTelemetry.update();
    }
}
