package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.bylazar.configurables.annotations.Configurable;
import com.bylazar.telemetry.JoinedTelemetry;
import com.bylazar.telemetry.PanelsTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.utility.EdgeDetector;

/**
 * Finds the blocker's blocking and open positions.
 *
 * Gamepad 1 A toggles the blocker between blocking and not blocking. Both positions are Panels values, and
 * editing one moves the servo right away if the blocker is in that state, so a position can be tuned live.
 *
 * Writes the servo directly instead of going through Transfer, so positions outside Transfer's clamp
 * (0.5 to 0.65) can be tried. Telemetry warns when a position is outside that clamp: copy the final
 * positions into Transfer (blockerClosedPos / blockerOpenPos) and widen the clamp if needed.
 *
 * Nothing moves during Init. At Start the blocker goes to the blocking position.
 */
@Configurable
@TeleOp(name = "Blocker Test", group = "Test")
public class BlockerTest extends OpMode {

    //---------------- Tunable (Panels) ----------------
    public static double blockingPos = 0.505; // Transfer.blockerClosedPos
    public static double openPos = 0.63;      // Transfer.blockerOpenPos

    //---------------- Constants ----------------
    // Transfer's clamp, only used for the warning
    private static final double TRANSFER_MIN = 0.5;
    private static final double TRANSFER_MAX = 0.65;

    //---------------- Hardware ----------------
    private Servo blocker;

    //---------------- Software ----------------
    private JoinedTelemetry joinedTelemetry;
    private boolean blocking = true;
    private double lastWritten = Double.NaN; // NaN so the first loop always writes
    private final EdgeDetector aButton = new EdgeDetector(() -> blocking = !blocking);

    //---------------- OpMode ----------------
    @Override
    public void init() {
        blocker = hardwareMap.get(Servo.class, "blocker");
        joinedTelemetry = new JoinedTelemetry(PanelsTelemetry.INSTANCE.getFtcTelemetry(), telemetry);
    }

    @Override
    public void init_loop() {
        joinedTelemetry.addLine("Blocker Test: press Start. The blocker goes to the blocking position.");
        joinedTelemetry.addLine("A toggles blocking / open. Edit blockingPos and openPos in Panels.");
        joinedTelemetry.update();
    }

    @Override
    public void start() {
        blocking = true;
        lastWritten = Double.NaN;
    }

    @Override
    public void loop() {
        aButton.update(gamepad1.a);

        // Re-read the Panels value every loop so edits move the servo live; only write when it changed
        double target = clamp01(blocking ? blockingPos : openPos);
        if (target != lastWritten) {
            blocker.setPosition(target);
            lastWritten = target;
        }

        joinedTelemetry.addData("State", blocking ? "BLOCKING" : "OPEN");
        joinedTelemetry.addData("Commanded position", "%.3f", target);
        joinedTelemetry.addData("blockingPos", "%.3f%s", blockingPos, clampWarning(blockingPos));
        joinedTelemetry.addData("openPos", "%.3f%s", openPos, clampWarning(openPos));
        joinedTelemetry.addLine("A toggles. Edit blockingPos / openPos in Panels.");
        joinedTelemetry.update();
    }

    //---------------- Helpers ----------------
    private static double clamp01(double pos) {
        return Math.max(0.0, Math.min(1.0, pos));
    }

    private static String clampWarning(double pos) {
        if (pos < TRANSFER_MIN || pos > TRANSFER_MAX) {
            return String.format("  (outside Transfer clamp %.3f-%.3f)", TRANSFER_MIN, TRANSFER_MAX);
        }
        return "";
    }
}
