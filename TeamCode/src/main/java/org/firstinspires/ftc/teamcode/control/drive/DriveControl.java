package org.firstinspires.ftc.teamcode.control.drive;

import com.bylazar.configurables.annotations.Configurable;
import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.control.Control;
import org.firstinspires.ftc.teamcode.subsystems.drive.Drive;
import org.firstinspires.ftc.teamcode.utility.driveUtility.Rates;
import org.firstinspires.ftc.teamcode.utility.EdgeDetector;

/** MainTeleOp's drive mappings and stick processing, for either drivetrain backend. */
@Configurable
public class DriveControl implements Control {
    public static double maxRate = 1.0;
    public static double moveCenterRate = 0.7;
    public static double moveExpo = 0.4;
    public static double deadzone = 0.01;
    public static double turnCenterRate = 0.375;
    public static double turnExpo = 0.4;

    private final Drive drive;
    protected final Gamepad gamepad;
    private final boolean ratesEnabled;
    private boolean fieldCentric;
    private boolean slowMode;
    private Rates moveRate, turnRate;
    private double cachedMax, cachedMoveCenter, cachedMoveExpo, cachedTurnCenter, cachedTurnExpo, cachedDeadzone;
    private double forward, right, turn;
    private final EdgeDetector slowToggle = new EdgeDetector(this::toggleSlowMode);
    private final EdgeDetector fieldToggle = new EdgeDetector(this::toggleFieldCentric);

    public DriveControl(Drive drive, Gamepad gamepad, boolean ratesEnabled, boolean fieldCentric) {
        this.drive = drive;
        this.gamepad = gamepad;
        this.ratesEnabled = ratesEnabled;
        this.fieldCentric = fieldCentric;
        slowToggle.isPressed = gamepad.b;
        fieldToggle.isPressed = gamepad.back;
        refreshRates();
    }

    private void refreshRates() {
        if (moveRate != null && cachedMax == maxRate && cachedMoveCenter == moveCenterRate
                && cachedMoveExpo == moveExpo && cachedTurnCenter == turnCenterRate
                && cachedTurnExpo == turnExpo && cachedDeadzone == deadzone) return;
        // Linear mode retains the same deadzone and maximum output, without curve shaping.
        Rates nextMove = new Rates(ratesEnabled ? moveCenterRate : maxRate, maxRate,
                ratesEnabled ? moveExpo : 0, deadzone);
        Rates nextTurn = new Rates(ratesEnabled ? turnCenterRate : maxRate, maxRate,
                ratesEnabled ? turnExpo : 0, deadzone);
        moveRate = nextMove;
        turnRate = nextTurn;
        cachedMax = maxRate;
        cachedMoveCenter = moveCenterRate;
        cachedMoveExpo = moveExpo;
        cachedTurnCenter = turnCenterRate;
        cachedTurnExpo = turnExpo;
        cachedDeadzone = deadzone;
    }

    @Override public void update() {
        updateButtons();
        refreshRates();
        double scale = slowMode ? 0.5 : 1;
        forward = moveRate.apply(-gamepad.left_stick_y) * scale;
        right = moveRate.apply(gamepad.left_stick_x) * scale;
        turn = turnRate.apply(gamepad.right_stick_x) * scale;
        if (fieldCentric) drive.fieldCentric(forward, right, turn);
        else drive.robotCentric(forward, right, turn);
    }

    /** Tests can supply their own mappings while sharing stick processing. */
    protected void updateButtons() {
        slowToggle.update(gamepad.b);
        fieldToggle.update(gamepad.back);
    }

    public void toggleSlowMode() { slowMode = !slowMode; }
    public void toggleFieldCentric() { fieldCentric = !fieldCentric; }

    @Override public void addTelemetry(Telemetry t) {
        t.addData("Rates", ratesEnabled);
        t.addData("Field Centric", fieldCentric);
        t.addData("Slow Mode", slowMode);
        t.addData("Stick forward/right/turn", "%.2f / %.2f / %.2f",
                -gamepad.left_stick_y, gamepad.left_stick_x, gamepad.right_stick_x);
        t.addData("Command forward/right/turn", "%.2f / %.2f / %.2f", forward, right, turn);
    }
}
