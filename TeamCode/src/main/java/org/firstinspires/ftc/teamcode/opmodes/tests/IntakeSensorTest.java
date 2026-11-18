package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.bylazar.telemetry.JoinedTelemetry;
import com.bylazar.telemetry.PanelsTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.firstinspires.ftc.teamcode.subsystems.intake.Spinner;
import org.firstinspires.ftc.teamcode.subsystems.intake.Transfer;
import org.firstinspires.ftc.teamcode.utility.intakeUtility.RealTimeBallFilter;

/**
 * Debugs the four intake beam sensors (distance_1..distance_4) and the ball count built from them.
 *
 * For each sensor it shows the raw getState() value, what Spinner calls "broken" (blocked), and how many
 * times the raw value has changed. Below that: the raw per-loop count, the filtered count, and whether the
 * spinner is running, because Spinner counts differently when it's running.
 *
 * Gamepad 1: RT runs the spinner and transfer in (power = how far the trigger is pressed), LT runs them out,
 * A resets the change counters. The blocker stays closed and the flywheel stays off, so nothing is shot.
 *
 * Doesn't change any robot code: it reads Spinner's own methods, plus the same DigitalChannels directly.
 */
@TeleOp(name = "Intake Sensor Test", group = "Test")
public class IntakeSensorTest extends OpMode {

    //---------------- Constants ----------------
    private static final String[] SENSOR_NAMES = {"distance_1", "distance_2", "distance_3", "distance_4"};
    private static final String[] SENSOR_LABELS = {"S1 entry", "S2", "S3", "S4 shooter"};
    private static final double TRIGGER_THRESHOLD = 0.1;
    private static final double TELEMETRY_INTERVAL_MS = 200;

    //---------------- Hardware ----------------
    private Robot robot;
    private Spinner spinner;
    private Transfer transfer;
    // The same devices Spinner reads, fetched again here so the raw getState() can be shown
    private final DigitalChannel[] sensors = new DigitalChannel[4];

    //---------------- Software ----------------
    private JoinedTelemetry joinedTelemetry;
    private final ElapsedTime telemetryTimer = new ElapsedTime();

    private final boolean[] raw = new boolean[4];
    private final boolean[] lastRaw = new boolean[4];
    private final int[] changes = new int[4]; // counted every loop, so short flips between telemetry updates show up
    private boolean firstRead = true;

    private double spinnerPow = 0.0; // what this OpMode told the spinner, which is what Spinner's count rules check
    private int rawCount = 0;
    // A second filter fed with this OpMode's copy of the raw count. If it agrees with Spinner's count, the copy
    // of the counting rules below still matches Spinner
    private final RealTimeBallFilter copyFilter = new RealTimeBallFilter();
    private int copyFilteredCount = 0;

    private boolean lastA = false;

    //---------------- OpMode ----------------
    @Override
    public void init() {
        robot = new Robot(hardwareMap, telemetry);
        spinner = robot.intake.spinner;
        transfer = robot.intake.transfer;
        for (int i = 0; i < 4; i++) {
            sensors[i] = hardwareMap.get(DigitalChannel.class, SENSOR_NAMES[i]);
        }

        joinedTelemetry = new JoinedTelemetry(PanelsTelemetry.INSTANCE.getFtcTelemetry(), telemetry);
    }

    @Override
    public void init_loop() {
        // Sensors can be read before Start without moving anything, so the check can begin right away
        readSensors();
        if (telemetryTimer.milliseconds() > TELEMETRY_INTERVAL_MS) {
            joinedTelemetry.addLine("Intake Sensor Test: press Start to run the intake.");
            joinedTelemetry.addLine("Empty intake now? Every sensor should say clear.");
            addSensorLines();
            joinedTelemetry.update();
            telemetryTimer.reset();
        }
    }

    @Override
    public void start() {
        try {
            robot.toInit(); // closes the blocker, stops the spinner and transfer
            telemetryTimer.reset();
        } catch (RuntimeException e) {
            stop();
            throw e;
        }
    }

    @Override
    public void loop() {
        try {
            updateIntake();
            robot.update(); // Spinner.update() writes the motor and runs updateBallCount()

            readSensors();
            rawCount = countBalls(spinner.isSensor1Broken(), spinner.isSensor2Broken(),
                    spinner.isSensor3Broken(), spinner.isSensor4Broken(), spinnerPow);
            copyFilteredCount = copyFilter.update(rawCount);

            if (gamepad1.a && !lastA) {
                for (int i = 0; i < 4; i++) changes[i] = 0;
            }
            lastA = gamepad1.a;

            if (telemetryTimer.milliseconds() > TELEMETRY_INTERVAL_MS) {
                addTelemetry();
                joinedTelemetry.update();
                telemetryTimer.reset();
            }
        } catch (RuntimeException e) {
            stop();
            throw e;
        }
    }

    @Override
    public void stop() {
        if (robot != null) robot.stop();
    }

    //---------------- Helpers ----------------
    // RT in, LT out, with power from the trigger, so a ball can be walked slowly past each sensor
    private void updateIntake() {
        double in = gamepad1.right_trigger;
        double out = gamepad1.left_trigger;

        if (in > TRIGGER_THRESHOLD && out <= TRIGGER_THRESHOLD) {
            spinnerPow = in;
        } else if (out > TRIGGER_THRESHOLD && in <= TRIGGER_THRESHOLD) {
            spinnerPow = -out;
        } else {
            spinnerPow = 0.0;
        }
        spinner.setSpinnerPow(spinnerPow);
        transfer.setTransferPow(spinnerPow);
    }

    private void readSensors() {
        for (int i = 0; i < 4; i++) {
            raw[i] = sensors[i].getState();
            if (!firstRead && raw[i] != lastRaw[i]) changes[i]++;
            lastRaw[i] = raw[i];
        }
        firstRead = false;
    }

    /**
     * Copy of the counting rules in Spinner.updateBallCount() (Spinner keeps its raw count private).
     * Walks the sensors from entry to shooter and groups blocked sensors that are next to each other.
     * Spinner stopped: every blocked sensor is a ball. Spinner running: a group of exactly 2 is one ball
     * sitting between two sensors, any other group counts one ball per sensor.
     */
    private static int countBalls(boolean s1, boolean s2, boolean s3, boolean s4, double spinnerPow) {
        boolean[] blocked = {s1, s2, s3, s4};
        int count = 0;
        int group = 0;
        for (int i = 0; i <= blocked.length; i++) {
            if (i < blocked.length && blocked[i]) {
                group++;
            } else if (group > 0) {
                count += (spinnerPow != 0 && group == 2) ? 1 : group;
                group = 0;
            }
        }
        return count;
    }

    private void addSensorLines() {
        boolean[] broken = {spinner.isSensor1Broken(), spinner.isSensor2Broken(),
                spinner.isSensor3Broken(), spinner.isSensor4Broken()};
        for (int i = 0; i < 4; i++) {
            joinedTelemetry.addData(SENSOR_LABELS[i], "raw %-5s -> code says %-7s | changes %d",
                    raw[i], broken[i] ? "BLOCKED" : "clear", changes[i]);
        }
        joinedTelemetry.addLine("Code: getState() true = blocked. Block each sensor by hand: raw should flip.");
        joinedTelemetry.addLine("Raw true when EMPTY and false when blocked = inverted (needs !getState()).");
        joinedTelemetry.addLine("Raw never changes = wiring, power, or wrong port in the config.");
        for (int i = 0; i < 4; i++) {
            joinedTelemetry.addData(SENSOR_NAMES[i] + " port", sensors[i].getConnectionInfo());
        }
    }

    private void addTelemetry() {
        String spinnerState = spinnerPow > 0 ? "IN" : spinnerPow < 0 ? "OUT" : "STOPPED";
        joinedTelemetry.addData("Spinner", "%s (power %.2f)", spinnerState, spinnerPow);
        joinedTelemetry.addData("Count rule", spinnerPow != 0
                ? "running: 2 blocked side by side = 1 ball"
                : "stopped: each blocked sensor = 1 ball");
        joinedTelemetry.addData("Raw count (this loop)", rawCount);
        joinedTelemetry.addData("Filtered count (Spinner)", spinner.getBallCount());
        joinedTelemetry.addData("Filtered count (test copy)", copyFilteredCount);
        joinedTelemetry.addData("Full (>= 4)", spinner.isFull());
        joinedTelemetry.addLine("RT in, LT out (trigger = power), A resets change counters");
        joinedTelemetry.addLine();
        addSensorLines();
    }
}
