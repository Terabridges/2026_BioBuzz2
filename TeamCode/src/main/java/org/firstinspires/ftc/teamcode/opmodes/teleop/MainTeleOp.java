package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.bylazar.configurables.annotations.Configurable;
import com.bylazar.telemetry.JoinedTelemetry;
import com.bylazar.telemetry.PanelsTelemetry;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
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
 *   Y         shoot macro                            OuttakeControl
 *   D-pad down  switch target Cell (close / far)     OuttakeControl
 * Gamepad 2
 *   A / X     reset the pose at reset point 1 / 2    here
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

    // Gamepad 2 pose resets, for when odometry drifts: put the robot on the spot, then press the button.
    // Reset point 1 (A) is the start pose above.
    // TODO pick the real spot for reset point 2 (X). Until then it's the start corner too
    public static double reset2X = FieldPoses.AUDIENCE_LEFT_CORNER_START.x();
    public static double reset2Y = FieldPoses.AUDIENCE_LEFT_CORNER_START.y();
    public static double reset2HeadingDeg = Math.toDegrees(FieldPoses.AUDIENCE_LEFT_CORNER_START.heading());

    private Robot robot;
    private DriveManager drive;
    private DriveControl driveControl;
    private Control[] mechanismControls;
    private OuttakeControl outtakeControl;
    private IntakeControl intakeControl;
    private JoinedTelemetry joinedTelemetry;
    private long lastTelemetry;

    private final EdgeDetector resetPose1 = new EdgeDetector(() -> setPose(startX, startY, startHeadingDeg));
    private final EdgeDetector resetPose2 = new EdgeDetector(() -> setPose(reset2X, reset2Y, reset2HeadingDeg));

    @Override public void init() {
        drive = new DriveManager(DRIVE_BACKEND);
        robot = new Robot(hardwareMap, telemetry);
        // The pose is only asked for after Start, once the drive (and its follower) exists
        outtakeControl = new OuttakeControl(robot, () -> drive.follower().pose(),
                () -> drive.follower().velocity(), gamepad1, gamepad2);
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
        addInitTelemetry();
    }

    private void addInitTelemetry() {
        drive.addTelemetry(telemetry);
        telemetry.addData("Rates", USE_RATES);
        telemetry.addData("Field Centric", START_FIELD_CENTRIC);
        telemetry.addData("Start pose without auto", "(%.2f, %.2f) %.0f deg", startX, startY, startHeadingDeg);
        telemetry.addLine("No auto first? Put the robot in the audience-left corner, facing away from the audience");
        telemetry.update();
    }

    @Override public void start() {
        try {
            robot.toInit();
            drive.start(hardwareMap, true);
            // Aiming depends on the pose, so without one from autonomous the robot has to start at the start pose
            if (!drive.resumedFromAuto()) setPose(startX, startY, startHeadingDeg);
            driveControl = new DriveControl(drive.drive(), gamepad1, USE_RATES, START_FIELD_CENTRIC);
        } catch (RuntimeException failure) {
            stop();
            throw failure;
        }
        lastTelemetry = System.nanoTime();
    }

    @Override public void loop() {
        long now = System.nanoTime();
        try {
            // Apply driving before potentially slow mechanism/camera hardware reads.
            resetPose1.update(gamepad2.a);
            resetPose2.update(gamepad2.x);
            driveControl.update();
            drive.update();
            for (Control control : mechanismControls) control.update();
            robot.update();
            // Only the pose, outtake and intake for now (no vision, drive or loop timing)
            if (now - lastTelemetry >= 200_000_000L) {
                Pose pose = drive.follower().pose();
                joinedTelemetry.addData("Pose", "(%.1f, %.1f) %.1f deg",
                        pose.x(), pose.y(), Math.toDegrees(pose.heading()));
                outtakeControl.addTelemetry(joinedTelemetry);
                intakeControl.addTelemetry(joinedTelemetry);
                joinedTelemetry.update();
                lastTelemetry = now;
            }
        } catch (RuntimeException failure) {
            stop();
            throw failure;
        }
    }

    @Override public void stop() {
        try {
            if (robot != null) robot.stop();
        } finally {
            if (drive != null) drive.stop();
        }
    }

    private void setPose(double x, double y, double headingDeg) {
        drive.follower().setPose(new Pose(x, y, Math.toRadians(headingDeg)));
    }
}
