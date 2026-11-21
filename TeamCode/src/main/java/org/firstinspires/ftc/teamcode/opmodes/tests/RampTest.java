package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.bylazar.configurables.annotations.Configurable;
import com.bylazar.telemetry.JoinedTelemetry;
import com.bylazar.telemetry.PanelsTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.utility.EdgeDetector;

/**
 * Finds the ramp's up and down positions.
 *
 * Gamepad 1 A toggles the ramp between up and down. Both positions and the right servo's offset are Panels
 * values, and editing one moves the servos right away, so positions can be tuned live.
 *
 * Sets up the servos the same way Ramp does (left reversed, right = left + rightOffset), but writes them
 * directly instead of going through Ramp, so positions outside Ramp's clamp can be tried. Ramp clamps the
 * left servo to 0.475 to 0.676 (0.7 minus the 0.024 offset); telemetry warns when a position is outside
 * that. Copy the final values into Ramp (rampUpPos / rampDownPos / rightRampOffset) and widen the clamp if
 * needed.
 *
 * Nothing moves during Init. At Start the ramp goes up, the same as Ramp.init().
 */
@Configurable
@TeleOp(name = "Ramp Test", group = "Test")
public class RampTest extends OpMode {

    //---------------- Tunable (Panels) ----------------
    public static double upPos = 0.685;       // Ramp.rampUpPos
    public static double downPos = 0.475;     // Ramp.rampDownPos
    public static double rightOffset = 0.024; // Ramp.rightRampOffset (right = left + offset)

    //---------------- Constants ----------------
    // Ramp's clamp on the left servo, only used for the warning
    private static final double RAMP_MIN = 0.475;
    private static final double RAMP_MAX = 0.7;

    //---------------- Hardware ----------------
    private Servo rampLeft;
    private Servo rampRight;

    //---------------- Software ----------------
    private JoinedTelemetry joinedTelemetry;
    private boolean up = true;
    // NaN so the first loop always writes
    private double lastLeft = Double.NaN;
    private double lastRight = Double.NaN;
    private final EdgeDetector aButton = new EdgeDetector(() -> up = !up);

    //---------------- OpMode ----------------
    @Override
    public void init() {
        rampLeft = hardwareMap.get(Servo.class, "ramp_left");
        rampRight = hardwareMap.get(Servo.class, "ramp_right");
        rampLeft.setDirection(Servo.Direction.REVERSE); // same as Ramp
        joinedTelemetry = new JoinedTelemetry(PanelsTelemetry.INSTANCE.getFtcTelemetry(), telemetry);
    }

    @Override
    public void init_loop() {
        joinedTelemetry.addLine("Ramp Test: press Start. The ramp goes up.");
        joinedTelemetry.addLine("A toggles up / down. Edit upPos, downPos, rightOffset in Panels.");
        joinedTelemetry.update();
    }

    @Override
    public void start() {
        up = true;
        lastLeft = Double.NaN;
        lastRight = Double.NaN;
    }

    @Override
    public void loop() {
        aButton.update(gamepad1.a);

        // Re-read the Panels values every loop so edits move the servos live; only write when they changed
        double left = clamp01(up ? upPos : downPos);
        double right = clamp01(left + rightOffset);
        if (left != lastLeft || right != lastRight) {
            rampLeft.setPosition(left);
            rampRight.setPosition(right);
            lastLeft = left;
            lastRight = right;
        }

        joinedTelemetry.addData("State", up ? "UP" : "DOWN");
        joinedTelemetry.addData("Left / right commanded", "%.3f / %.3f", left, right);
        joinedTelemetry.addData("upPos", "%.3f%s", upPos, clampWarning(upPos));
        joinedTelemetry.addData("downPos", "%.3f%s", downPos, clampWarning(downPos));
        joinedTelemetry.addData("rightOffset", "%.3f", rightOffset);
        joinedTelemetry.addLine("A toggles. Edit upPos / downPos / rightOffset in Panels.");
        joinedTelemetry.update();
    }

    //---------------- Helpers ----------------
    private static double clamp01(double pos) {
        return Math.max(0.0, Math.min(1.0, pos));
    }

    // Matches Ramp.setRamp(): with a positive offset the left servo's max is lowered so the right stays in range
    private static String clampWarning(double pos) {
        double min = rightOffset < 0 ? RAMP_MIN - rightOffset : RAMP_MIN;
        double max = rightOffset < 0 ? RAMP_MAX : RAMP_MAX - rightOffset;
        if (pos < min || pos > max) {
            return String.format("  (outside Ramp clamp %.3f-%.3f)", min, max);
        }
        return "";
    }
}
