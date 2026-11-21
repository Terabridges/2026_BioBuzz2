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
    // MainTeleOp drive configuration. Aiming needs the robot's field pose, which only the PEDRO backend has
    private static final DriveManager.Backend DRIVE_BACKEND = DriveManager.Backend.PEDRO;
    private static final boolean USE_RATES = false;
    private static final boolean START_FIELD_CENTRIC = false;

    //---------------- Tunable (Panels) ----------------
    // Start pose when autonomous didn't run first, same as Test Shot: robot in the audience-left corner
    // (the field's 0, 0 corner), back against the audience wall, left side against the left wall, facing
    // away from the audience. The pose is the robot's center: half its width in x, half its length in y
    public static double startX = FieldPoses.AUDIENCE_LEFT_CORNER_START.x();   // 7.58 in.
    public static double startY = FieldPoses.AUDIENCE_LEFT_CORNER_START.y();   // 8.19 in.
    public static double startHeadingDeg = Math.toDegrees(FieldPoses.AUDIENCE_LEFT_CORNER_START.heading());
    // How long gamepad 2 Y holds the drive still while the Pinpoint recalibrates its gyro. goBILDA says
    // it takes about 0.25 s; Pedro waits 0.5 s after its own calibration at INIT
    public static double imuCalibrateSec = 0.5;


    private Robot robot;
    private DriveManager drive;
    private DriveControl driveControl;
    private Control[] mechanismControls;
    private OuttakeControl outtakeControl;
    private IntakeControl intakeControl;
    private JoinedTelemetry joinedTelemetry;
    private long lastTelemetry;

    // Our alliance: the hive the turret aims at, its shooting zone, and which wall the reset corners are on.
    // Gamepad 2 Start switches it, in INIT or during the match. It's static so it's remembered for the next
    // run (until the Robot Controller app restarts), so it only needs setting once per event, not every match
    private static Alliance alliance = Alliance.RED;
    private final EdgeDetector switchAlliance = new EdgeDetector(this::switchAlliance);

    // Gamepad 2 pose resets, for when odometry drifts: drive into the corner on our alliance's side wall,
    // back against the end wall, then press. X = close corner (audience wall), A = far corner (rear wall).
    // The alliance is the one the turret aims for
    private final EdgeDetector resetClose = new EdgeDetector(() -> resetToCorner(false));
    private final EdgeDetector resetFar = new EdgeDetector(() -> resetToCorner(true));
    // Gamepad 2 Y recalibrates the Pinpoint's gyro to slow heading drift. It doesn't move the pose, so press
    // it together with X or A in the corner to fix both, or skip it when there's no time to sit still
    private final EdgeDetector recalibrateImu = new EdgeDetector(this::recalibrateImu);
    private long imuCalibratingUntil;

    @Override public void init() {
        drive = new DriveManager(DRIVE_BACKEND);
        // A fresh Pinpoint calibrates here, before the turret centers at START.
        drive.initialize(hardwareMap, true);
        robot = new Robot(hardwareMap, telemetry);
        // The pose and velocity are only asked for after Start, once the drive is running
        outtakeControl = new OuttakeControl(robot, () -> drive.follower().pose(),
                () -> drive.follower().velocity(), gamepad1, gamepad2);
        outtakeControl.setAlliance(alliance);
        intakeControl = new IntakeControl(robot.intake, gamepad1, gamepad2, outtakeControl::isFiring);
        // Outtake goes before intake: it decides whether to fire, and the intake feeds the balls when it does
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
        drive.start(); // the drive was set up in init(); this starts driving
        // Aiming depends on the pose, so without one from autonomous the robot has to start at the start pose
        if (!drive.resumedFromAuto()) setPose(startX, startY, startHeadingDeg);
        driveControl = new DriveControl(drive.drive(), gamepad1, USE_RATES, START_FIELD_CENTRIC);
        lastTelemetry = System.nanoTime();
    }

    @Override public void loop() {
        long now = System.nanoTime();
        // Apply driving before potentially slow mechanism/camera hardware reads.
        switchAlliance.update(gamepad2.start);
        resetClose.update(gamepad2.x);
        resetFar.update(gamepad2.a);
        recalibrateImu.update(gamepad2.y);
        // The gyro samples its new zero while the robot is still, so the sticks are ignored until it's done
        if (isCalibratingImu(now)) drive.drive().robotCentric(0, 0, 0);
        else driveControl.update();
        drive.update();
        for (Control control : mechanismControls) control.update();
        robot.update();
        // Only the pose, outtake and intake for now (no vision, drive or loop timing)
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

    // Step 1 of shooting on the move: check the odometry velocity before using it to lead shots.
    // The lead math needs velocity in the FIELD frame (x along the audience wall, y away from it).
    // Test: drive straight forward with the robot facing different ways. If the velocity is field-frame,
    // "Travel dir" matches the heading every time. If it's robot-frame, it reads about 0 deg every time.
    // Also watch "Speed" with the robot sitting still: that's the noise the lead would have to ignore.
    private void addVelocityTelemetry(Pose pose) {
        Velocity velocity = drive.follower().velocity();
        double speed = Math.hypot(velocity.vx, velocity.vy);
        joinedTelemetry.addData("Velocity x / y", "%+.1f / %+.1f in/s", velocity.vx, velocity.vy);
        joinedTelemetry.addData("Speed", "%.2f in/s", speed);
        // Direction is meaningless when (nearly) still, so only show it while actually moving
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

    // A corner reset is a fresh start for aiming, so the gamepad 2 shot offsets go back to zero too
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
