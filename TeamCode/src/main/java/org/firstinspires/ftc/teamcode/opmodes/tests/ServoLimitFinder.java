package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.subsystems.intake.Ramp;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Turret;

/**
 * Moves one mechanism at a time to a configurable position so each one's limits can be found.
 * Paired mechanisms (ramp, turret) drive both servos together, with the subsystem's offset added
 * to the right servo. Only the selected mechanism is powered. Directions and PWM ranges match the
 * subsystems, so the positions found here can be copied straight into the min/max constants.
 */
@Configurable
@TeleOp(name = "ServoLimitFinder", group = "Test")
public class ServoLimitFinder extends OpMode {

    private Robot robot = new Robot(hardwareMap, telemetry);

    public static double position = 0.5;
    public static double step = 0.01;

    private static class Mechanism {
        final String name;
        final Servo left;
        final Servo right; // null for single servos
        final double rightOffset;
        double lastCommanded = Double.NaN;
        boolean enabled = false;
        boolean missing;

        Mechanism(String name, Servo left, Servo right, double rightOffset, boolean missing) {
            this.name = name;
            this.left = left;
            this.right = right;
            this.rightOffset = rightOffset;
            this.missing = missing;
        }

        // Keeps both servos inside [0, 1] once the offset is applied, same as the subsystems
        double clamp(double pos) {
            double lo = Math.max(0.0, -rightOffset);
            double hi = Math.min(1.0, 1.0 - rightOffset);
            return Math.max(lo, Math.min(pos, hi));
        }
    }

    private Mechanism[] mechanisms;
    private int selected = 0;

    private final Gamepad current = new Gamepad();
    private final Gamepad previous = new Gamepad();

    @Override
    public void init() {
        mechanisms = new Mechanism[]{
                single("hood"),
                single("blocker"),
                pair("ramp", "ramp_left", "ramp_right", robot.intake.ramp.getRightRampOffset()),
                pair("turret", "turret_left", "turret_right", robot.outtake.turret.getRightTurretOffset())
        };

        // Match Ramp
        Mechanism ramp = mechanisms[2];
        if (ramp.left != null) ramp.left.setDirection(Servo.Direction.REVERSE);
        // Match Turret
        Mechanism turret = mechanisms[3];
        PwmControl.PwmRange turretRange = new PwmControl.PwmRange(500, 2500);
        if (turret.left instanceof PwmControl) ((PwmControl) turret.left).setPwmRange(turretRange);
        if (turret.right instanceof PwmControl) ((PwmControl) turret.right).setPwmRange(turretRange);

        for (Mechanism m : mechanisms) disable(m);
    }

    @Override
    public void loop() {
        previous.copy(current);
        current.copy(gamepad1);

        if (edge(current.dpad_right, previous.dpad_right)) select(selected + 1);
        if (edge(current.dpad_left, previous.dpad_left)) select(selected - 1);

        if (edge(current.dpad_up, previous.dpad_up)) nudge(step);
        if (edge(current.dpad_down, previous.dpad_down)) nudge(-step);

        if (edge(current.a, previous.a)) apply();
        if (edge(current.b, previous.b)) disable(mechanisms[selected]);

        showTelemetry();
    }

    @Override
    public void stop() {
        for (Mechanism m : mechanisms) disable(m);
    }

    private Mechanism single(String name) {
        Servo s = hardwareMap.tryGet(Servo.class, name);
        return new Mechanism(name, s, null, 0.0, s == null);
    }

    private Mechanism pair(String name, String leftName, String rightName, double rightOffset) {
        Servo l = hardwareMap.tryGet(Servo.class, leftName);
        Servo r = hardwareMap.tryGet(Servo.class, rightName);
        return new Mechanism(name, l, r, rightOffset, l == null || r == null);
    }

    // Switching mechanisms cuts power to the old one so only one is ever driven
    private void select(int index) {
        disable(mechanisms[selected]);
        selected = (index + mechanisms.length) % mechanisms.length;
    }

    private void nudge(double delta) {
        position += delta;
        apply();
    }

    private void apply() {
        Mechanism m = mechanisms[selected];
        position = m.clamp(position);
        if (m.missing) return;

        write(m.left, position);
        if (m.right != null) write(m.right, position + m.rightOffset);
        m.lastCommanded = position;
        m.enabled = true;
    }

    private static void write(Servo s, double pos) {
        if (s instanceof PwmControl) ((PwmControl) s).setPwmEnable();
        s.setPosition(pos);
    }

    private static void disable(Mechanism m) {
        if (m.left instanceof PwmControl) ((PwmControl) m.left).setPwmDisable();
        if (m.right instanceof PwmControl) ((PwmControl) m.right).setPwmDisable();
        m.enabled = false;
    }

    private void showTelemetry() {
        Mechanism sel = mechanisms[selected];
        telemetry.addData("Selected", sel.name);
        telemetry.addData("Position (configurable)", "%.3f", position);
        if (sel.right != null) {
            telemetry.addData("Right offset", "%+.3f", sel.rightOffset);
        }
        telemetry.addLine();
        for (int i = 0; i < mechanisms.length; i++) {
            Mechanism m = mechanisms[i];
            String state;
            if (m.missing) {
                state = "not in config";
            } else if (Double.isNaN(m.lastCommanded)) {
                state = "--" + (m.enabled ? "  ON" : "  off");
            } else if (m.right != null) {
                state = String.format("L %.3f / R %.3f", m.lastCommanded, m.lastCommanded + m.rightOffset)
                        + (m.enabled ? "  ON" : "  off");
            } else {
                state = String.format("%.3f", m.lastCommanded) + (m.enabled ? "  ON" : "  off");
            }
            telemetry.addData((i == selected ? "> " : "  ") + m.name, state);
        }
        telemetry.addLine();
        telemetry.addLine("Dpad L/R: select (old one goes limp)");
        telemetry.addLine("A: move to position, Dpad U/D: nudge by step and move");
        telemetry.addLine("B: disable selected");
        telemetry.update();
    }

    private static boolean edge(boolean now, boolean prevVal) {
        return now && !prevVal;
    }
}
