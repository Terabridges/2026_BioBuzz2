package org.firstinspires.ftc.teamcode.control.drive;

import static org.junit.Assert.*;
import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.teamcode.subsystems.drive.Drive;
import org.junit.Test;

/** Input directions and optional settings must behave consistently for either backend. */
public class DriveControlTest {
    private static class RecordingDrive implements Drive {
        double forward, right, turn;
        boolean field;
        @Override public void robotCentric(double f, double r, double t) {
            forward = f; right = r; turn = t; field = false;
        }
        @Override public void fieldCentric(double f, double r, double t) {
            robotCentric(f, r, t); field = true;
        }
    }

    @Test public void linearModePreservesDirectionsAndReleaseImmediatelyCommandsZero() {
        RecordingDrive drive = new RecordingDrive();
        Gamepad gamepad = new Gamepad();
        DriveControl control = new DriveControl(drive, gamepad, false, false);
        gamepad.left_stick_y = -1;
        gamepad.left_stick_x = 1;
        gamepad.right_stick_x = 1;
        control.update();
        assertEquals(1, drive.forward, 0);
        assertEquals(1, drive.right, 0);
        assertEquals(1, drive.turn, 0);
        gamepad.left_stick_y = gamepad.left_stick_x = gamepad.right_stick_x = 0;
        control.update();
        assertEquals(0, drive.forward, 0);
        assertEquals(0, drive.right, 0);
        assertEquals(0, drive.turn, 0);
    }

    @Test public void ratesChangeSensitivityWithoutChangingFullStickOrDirection() {
        Gamepad gamepad = new Gamepad();
        gamepad.left_stick_y = -0.5f;
        gamepad.right_stick_x = 0.5f;
        RecordingDrive linear = new RecordingDrive(), shaped = new RecordingDrive();
        DriveControl raw = new DriveControl(linear, gamepad, false, false);
        DriveControl rates = new DriveControl(shaped, gamepad, true, false);
        raw.update(); rates.update();
        assertTrue(shaped.forward > 0 && shaped.forward < linear.forward);
        assertTrue(shaped.turn > 0 && shaped.turn < linear.turn);
        gamepad.left_stick_y = -1;
        gamepad.right_stick_x = 1;
        raw.update(); rates.update();
        assertEquals(linear.forward, shaped.forward, 0);
        assertEquals(linear.turn, shaped.turn, 0);
    }

    @Test public void heldStartButtonsDoNotToggleAndMatchMappingsUseRisingEdges() {
        RecordingDrive drive = new RecordingDrive();
        Gamepad gamepad = new Gamepad();
        gamepad.back = true;
        gamepad.b = true;
        gamepad.left_stick_y = -1;
        DriveControl control = new DriveControl(drive, gamepad, true, true);
        control.update();
        assertTrue(drive.field);
        assertEquals(1, drive.forward, 0);
        gamepad.back = gamepad.b = false;
        control.update();
        gamepad.back = gamepad.b = true;
        control.update(); control.update();
        assertFalse(drive.field);
        assertEquals(0.5, drive.forward, 0);
    }

    @Test public void testControlsCanReplaceMatchButtonsWithoutChangingStickProcessing() {
        RecordingDrive drive = new RecordingDrive();
        Gamepad gamepad = new Gamepad();
        DriveControl testControl = new DriveControl(drive, gamepad, false, false) {
            @Override protected void updateButtons() {} // A test with no toggle bindings.
        };
        gamepad.b = gamepad.back = true;
        gamepad.left_stick_y = -1;
        testControl.update();
        assertFalse(drive.field);
        assertEquals(1, drive.forward, 0);
    }
}
