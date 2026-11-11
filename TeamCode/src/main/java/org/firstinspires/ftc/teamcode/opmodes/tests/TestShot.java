package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.bylazar.configurables.annotations.Configurable;
import com.bylazar.telemetry.JoinedTelemetry;
import com.bylazar.telemetry.PanelsTelemetry;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
import org.firstinspires.ftc.teamcode.control.drive.DriveControl;
import org.firstinspires.ftc.teamcode.pedro.FollowerManager;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.subsystems.drive.PedroDrive;
import org.firstinspires.ftc.teamcode.subsystems.intake.Intake;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Shooter;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Turret;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;
import org.firstinspires.ftc.teamcode.utility.shooterUtility.ShooterData;
import org.firstinspires.ftc.teamcode.utility.shooterUtility.TurretAim;

import java.util.Locale;

/**
 * Tests the ShooterData tables: the same as the Shot Data Collector, but RPM and hood come from ShooterData for
 * the current distance, every loop, and nothing is saved. Drive anywhere, wait for READY, and fire.
 *
 * <p>Distance comes from odometry (TurretAim: turret pivot → the Cell for the half of the field the robot is on).
 * Set manualDistance in Panels to a tape-measured value to use that instead; 0 goes back to odometry.
 *
 * <p>Init: X = blue, B = red (default red).
 * <p>Gamepad 1 after Start:
 * <pre>
 *   sticks            drive (Back = field-centric; no slow mode here, B is taken)
 *   B                 switch alliance (red / blue)
 *   A                 flywheel on/off (RPM and hood follow the tables while it's on)
 *   RB                fire one ball (only when the flywheel is at the table's RPM)
 *   RT / LT           intake in / out, to load balls
 *   X                 turret: locked forward / tracking the Cell
 * </pre>
 */
@Configurable
@TeleOp(name = "Test Shot", group = "Test")
public class TestShot extends OpMode {

    //---------------- Tunable (Panels) ----------------
    public static double manualDistance = 0;    // inches; 0 = use odometry
    public static double readyTolerance = 50;   // RPM from target before a shot is allowed
    public static double feedTimeMs = 1000;     // how long one shot runs the feed. Same as the Shot Data Collector
    public static double feedPower = 1.0;

    // Start pose when autonomous didn't run first, same as the Shot Data Collector: robot in the audience-left
    // corner, back against the audience wall, left side against the left wall, facing away from the audience
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
    private final ShooterData shooterData = new ShooterData();
    private JoinedTelemetry joinedTelemetry;

    //---------------- Software ----------------
    private Alliance alliance = Alliance.RED;
    private boolean flywheelOn = false;
    private boolean tracking = false;
    private boolean feeding = false;
    private int shotCount = 0;
    private double distance = 0;
    private double tableRPM = 0;              // from ShooterData for the current distance
    private double tableHood = Double.NaN;    // from ShooterData for the current distance
    private double appliedHood = Double.NaN;  // last hood position sent to the Shooter

    private final ElapsedTime feedTimer = new ElapsedTime();
    private final ElapsedTime telemetryTimer = new ElapsedTime();

    private static final double TRIGGER_THRESHOLD = 0.1;
    // The table's hood value changes a tiny bit every time odometry moves. Only send it to the servo when it
    // has moved more than this, so the servo isn't rewritten every loop
    private static final double HOOD_DEADBAND = 0.002;

    //---------------- Buttons ----------------
    private final EdgeDetector toggleFlywheel = new EdgeDetector(() -> flywheelOn = !flywheelOn);
    private final EdgeDetector fire = new EdgeDetector(this::fire);
    private final EdgeDetector toggleTracking = new EdgeDetector(() -> tracking = !tracking);
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
        // Same drive settings as MainTeleOp: rates on, robot-centric to start. B switches alliance here, so
        // DriveControl's buttons are replaced: only Back (field-centric), no slow mode
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
        fire.update(gamepad1.right_bumper);
        toggleTracking.update(gamepad1.x);
        allianceButton.update(gamepad1.b);
    }

    private void switchAlliance() {
        alliance = alliance == Alliance.RED ? Alliance.BLUE : Alliance.RED;
        turretAim.setAlliance(alliance);
    }

    // RPM and hood from the tables for the current distance. ShooterData returns INVALID outside its range
    // (0–1000 in.); then the flywheel stops and the hood stays where it is
    private void updateShooter() {
        double rpm = shooterData.getRPMVal(distance);
        double hood = shooterData.getAngleVal(distance);
        boolean valid = rpm != shooterData.INVALID && hood != shooterData.INVALID;

        tableRPM = valid ? rpm : 0;
        tableHood = valid ? hood : Double.NaN;
        shooter.setTargetRPM(flywheelOn && valid ? tableRPM : 0);

        if (valid && (Double.isNaN(appliedHood) || Math.abs(hood - appliedHood) > HOOD_DEADBAND)) {
            shooter.setHoodPos(hood);
            appliedHood = shooter.getHoodPos(); // Shooter clamps it to its limits
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
        return flywheelOn && tableRPM > 0 && Math.abs(shooter.getCurrentRPM() - tableRPM) <= readyTolerance;
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

    private void addTelemetry(Pose pose) {
        joinedTelemetry.addData("Alliance (B to switch)", alliance);
        joinedTelemetry.addData("Distance (in)", "%.1f  (%s)", distance,
                manualDistance > 0 ? "manual" : "odometry, " + turretAim.getCell() + " Cell");
        if (Double.isNaN(tableHood)) {
            joinedTelemetry.addLine("Table: distance out of range, flywheel held off");
        } else {
            joinedTelemetry.addData("Table RPM / hood", "%.0f / %.3f", tableRPM, tableHood);
        }
        joinedTelemetry.addData("Current RPM", "%.0f", shooter.getCurrentRPM());
        joinedTelemetry.addData("Hood (sent)", "%.3f", appliedHood);
        joinedTelemetry.addData("Flywheel", flywheelOn ? (isReady() ? "READY" : "spinning up") : "off");
        joinedTelemetry.addData("Shots", shotCount);
        joinedTelemetry.addData("Turret", tracking
                ? String.format(Locale.US, "tracking, %.1f deg", Math.toDegrees(turretAim.getTurretAngle()))
                : "locked forward");
        joinedTelemetry.addData("Pose", "(%.1f, %.1f) %.1f deg", pose.x(), pose.y(), Math.toDegrees(pose.heading()));
        joinedTelemetry.update();
    }
}
