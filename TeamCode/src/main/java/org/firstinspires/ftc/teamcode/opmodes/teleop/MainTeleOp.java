package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.bylazar.configurables.annotations.Configurable;
import com.bylazar.telemetry.JoinedTelemetry;
import com.bylazar.telemetry.PanelsTelemetry;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
import org.firstinspires.ftc.teamcode.autonomous.paths.FieldPoses;
import org.firstinspires.ftc.teamcode.config.RobotGeometry;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.control.drive.DriveControl;
import org.firstinspires.ftc.teamcode.subsystems.drive.DriveManager;
import org.firstinspires.ftc.teamcode.control.intake.IntakeControl;
import org.firstinspires.ftc.teamcode.control.outtake.OuttakeControl;
import org.firstinspires.ftc.teamcode.control.vision.VisionControl;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;

/**
 * The match TeleOp. The buttons live in the Control classes (see doc/gamepadControls.md):
 * <pre>
 * Gamepad 1
 *   sticks    drive (robot-centric)                  DriveControl
 *   LB        slow mode while held                   DriveControl
 *   RT / LT   intake in / out                        IntakeControl
 *   A         toggle ramp                            IntakeControl
 *   B         toggle flywheel                        OuttakeControl
 *   X         toggle auto shoot                      OuttakeControl
 *   Y         manual shot: fires while held          OuttakeControl
 *   D-pad down  switch target Cell (close / far)     OuttakeControl
 * Gamepad 2
 *   Start     switch alliance (red / blue), in INIT or during the match   here
 *   X / A     reset the pose: close / far corner, and clear the shot offsets   here
 *   Y         recalibrate the Pinpoint gyro (drive held still 0.5 s)          here
 *   D-pad, bumpers, B    shot offsets                OuttakeControl
 * </pre>
 */
@Configurable
@TeleOp(name = "MainTeleOp", group = "Main")
public class MainTeleOp extends OpMode {

    private static final DriveManager.Backend DRIVE_BACKEND = DriveManager.Backend.PEDRO;
    private static final boolean USE_RATES = false;
    private static final boolean START_FIELD_CENTRIC = false;

    //---------------- Tunable (Panels) ----------------
    // Fresh follower starts in the audience-left corner with the wall offset on both axes.
    public static double startX = FieldPoses.AUDIENCE_LEFT_CORNER_START.x() + RobotGeometry.wall;
    public static double startY = FieldPoses.AUDIENCE_LEFT_CORNER_START.y() + RobotGeometry.wall;
    public static double startHeadingDeg = Math.toDegrees(FieldPoses.AUDIENCE_LEFT_CORNER_START.heading());
    public static double imuCalibrateSec = 0.5;

    private Robot robot;
    private DriveManager drive;
    private DriveControl driveControl;
    private Control[] mechanismControls;
    private OuttakeControl outtakeControl;
    private IntakeControl intakeControl;

    private JoinedTelemetry joinedTelemetry;
    private long lastTelemetry;

    private static Alliance alliance = Alliance.RED;
    private long imuCalibratingUntil;

    private final EdgeDetector switchAlliance = new EdgeDetector(this::switchAlliance);
    private final EdgeDetector resetClose = new EdgeDetector(() -> resetToCorner(false));
    private final EdgeDetector resetFar = new EdgeDetector(() -> resetToCorner(true));
    private final EdgeDetector recalibrateImu = new EdgeDetector(this::recalibrateImu);

    @Override public void init() {
        drive = new DriveManager(DRIVE_BACKEND);
        drive.initialize(hardwareMap, true);

        robot = new Robot(hardwareMap, telemetry);
        // The pose and velocity are only asked for after Start, once the drive is running
        robot.outtake.setMotionSource(() -> drive.follower().pose(), () -> drive.follower().velocity());
        outtakeControl = new OuttakeControl(robot.outtake, robot.intake.spinner, gamepad1, gamepad2);
        outtakeControl.setAlliance(alliance);
        intakeControl = new IntakeControl(robot.intake, gamepad1);
        mechanismControls = new Control[]{
                new VisionControl(robot.vision, gamepad1, gamepad2),
                outtakeControl,
                intakeControl};

        joinedTelemetry = new JoinedTelemetry(PanelsTelemetry.INSTANCE.getFtcTelemetry(), telemetry);
        addInitTelemetry();
    }

    @Override public void init_loop() {
        switchAlliance.update(gamepad2.start);
        addInitTelemetry();
    }

    private void addInitTelemetry() {
        telemetry.addData("Alliance", "%s (gamepad 2 Start to switch)", alliance);
        drive.addTelemetry(telemetry);
        telemetry.addData("Rates", USE_RATES);
        telemetry.addData("Field Centric", START_FIELD_CENTRIC);
        telemetry.addData("Start pose without auto", "(%.2f, %.2f) %.0f deg", startX, startY, startHeadingDeg);
        telemetry.addLine("Without auto, place the robot in the audience-left corner before INIT, facing away from the audience");
        telemetry.update();
    }

    @Override public void start() {
        robot.toInit();
        drive.start();

        if (!drive.resumedFromAuto()) setPose(startX, startY, startHeadingDeg);
        driveControl = new DriveControl(drive.drive(), gamepad1, USE_RATES, START_FIELD_CENTRIC);
        lastTelemetry = System.nanoTime();
    }

    @Override public void loop() {
        long now = System.nanoTime();

        switchAlliance.update(gamepad2.start);
        resetClose.update(gamepad2.x);
        resetFar.update(gamepad2.a);
        recalibrateImu.update(gamepad2.y);

        if (isCalibratingImu(now)) drive.drive().robotCentric(0, 0, 0);
        else driveControl.update();
        drive.update();

        for (Control control : mechanismControls) control.update();
        robot.update();

        if (now - lastTelemetry >= 200_000_000L) {
            Pose pose = drive.follower().pose();
            joinedTelemetry.addData("Alliance", alliance);
            if (isCalibratingImu(now)) joinedTelemetry.addLine("CALIBRATING IMU: hold still");
            joinedTelemetry.addData("Pose", "(%.1f, %.1f) %.1f deg",
                    pose.x(), pose.y(), Math.toDegrees(pose.heading()));
            addVelocityTelemetry(pose);
            outtakeControl.addTelemetry(joinedTelemetry);
            intakeControl.addTelemetry(joinedTelemetry);
            joinedTelemetry.update();
            lastTelemetry = now;
        }
    }

    @Override public void stop() {
        try {
            if (robot != null) robot.stop();
        } finally {
            if (drive != null) drive.stop();
        }
    }

    private void addVelocityTelemetry(Pose pose) {
        Velocity velocity = drive.follower().velocity();
        double speed = Math.hypot(velocity.vx, velocity.vy);
        joinedTelemetry.addData("Velocity x / y", "%+.1f / %+.1f in/s", velocity.vx, velocity.vy);
        joinedTelemetry.addData("Speed", "%.2f in/s", speed);
        if (speed > 5) {
            joinedTelemetry.addData("Travel dir / heading", "%.0f / %.0f deg",
                    Math.toDegrees(Math.atan2(velocity.vy, velocity.vx)), Math.toDegrees(pose.heading()));
        } else {
            joinedTelemetry.addData("Travel dir / heading", "(too slow) / %.0f deg", Math.toDegrees(pose.heading()));
        }
    }

    private void switchAlliance() {
        alliance = alliance == Alliance.RED ? Alliance.BLUE : Alliance.RED;
        outtakeControl.setAlliance(alliance);
    }

    private void resetToCorner(boolean far) {
        drive.follower().setPose(FieldPoses.allianceCorner(alliance, far));
        outtakeControl.clearOffsets();
    }

    private void recalibrateImu() {
        drive.recalibrateImu();
        imuCalibratingUntil = System.nanoTime() + (long) (imuCalibrateSec * 1e9);
    }

    private boolean isCalibratingImu(long now) {
        return now - imuCalibratingUntil < 0;
    }

    private void setPose(double x, double y, double headingDeg) {
        drive.follower().setPose(new Pose(x, y, Math.toRadians(headingDeg)));
    }
}
