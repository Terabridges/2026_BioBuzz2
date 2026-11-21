package org.firstinspires.ftc.teamcode.control.outtake;

import static org.junit.Assert.assertEquals;

import org.firstinspires.ftc.teamcode.subsystems.outtake.Lights;
import org.junit.Test;

/** Lights by ball count: off when empty, orange with 1 to 3 balls, green when full (4). */
public class OuttakeLightsTest {
    @Test public void emptyIsOff() {
        assertEquals(Lights.OFF, Lights.colorForBallCount(0), 0);
    }

    @Test public void oneToThreeBallsIsOrange() {
        for (int balls = 1; balls <= 3; balls++) {
            assertEquals(Lights.ORANGE, Lights.colorForBallCount(balls), 0);
        }
    }

    @Test public void fullIsGreen() {
        assertEquals(Lights.GREEN, Lights.colorForBallCount(4), 0);
    }
}
