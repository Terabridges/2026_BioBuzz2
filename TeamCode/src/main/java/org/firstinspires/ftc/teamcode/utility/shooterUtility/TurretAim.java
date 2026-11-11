package org.firstinspires.ftc.teamcode.utility.shooterUtility;

import com.bylazar.configurables.annotations.Configurable;
import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;

/**
 * Works out where the turret should point from the robot's field pose. Pure math: it never touches hardware.
 * Call {@link #update} once per loop with the follower's pose, then read the results.
 *
 * <p>Team field frame (Pedro): inches, 0–144, (0, 0) is the audience-side corner on the left, +x runs right along
 * the audience wall, +y runs away from the audience. Heading is in radians, counterclockwise positive.
 *
 * <p>Which Cell: the robot can only shoot the Cell on its own half of the field, so it aims at the close Cell
 * while it's on the audience half (y below 72) and the far Cell on the other half, whether or not that Cell is
 * the one facing up. A band around y = 72 stops it flicking back and forth while the robot sits on the line.
 */
@Configurable
public class TurretAim {

    //---------------- Field (red; blue is mirrored across x = 72) ----------------
    // Far Cell = close Cell mirrored across the hive's tilt line (y = 72): y = 144 - 50.7. Not measured yet
//    public static double redFarCellX = 56.8, redFarCellY = 93.3;     // away from the audience
//    public static double redCloseCellX = 56.8, redCloseCellY = 50.7; // toward the audience. Measured
    public static double redFarCellX = 59.25, redFarCellY = 87.8;     // away from the audience
    public static double redCloseCellX = 59.25, redCloseCellY = 56.2; // toward the audience. Measured

    public static double cellSwitchY = 72;    // the hive pivot line
    public static double cellSwitchBand = 6;  // inches past the line before switching Cells

    //---------------- Robot ----------------
    // Where the turret pivot sits relative to the robot's center, in the robot's frame (inches).
    // Forward is toward the front of the robot, left is toward its left side.
    // Negative forward = toward the back. Robot is 416 mm long x 385 mm wide; the turret center is 189 mm
    // from the back, so 208 - 189 = 19 mm behind the center, and centered side to side.
    public static double turretOffsetForward = -19 / 25.4; // -0.748 in.
    public static double turretOffsetLeft = 0;

    public enum Cell { CLOSE, FAR }

    private static final double FIELD_SIZE = 144;

    //---------------- State ----------------
    private Alliance alliance;
    private Cell cell = null; // picked on the first update from which half the robot is on

    //---------------- Results ----------------
    private double turretX, turretY;
    private double targetX, targetY;
    private double distance;
    private double turretAngle;

    //---------------- Constructor ----------------
    public TurretAim(Alliance alliance) {
        this.alliance = alliance;
    }

    //---------------- Methods ----------------
    public void update(Pose pose) {
        update(pose.x(), pose.y(), pose.heading());
    }

    /**
     * @param x       robot center x, inches
     * @param y       robot center y, inches
     * @param heading robot heading, radians, counterclockwise positive
     */
    public void update(double x, double y, double heading) {
        updateCell(y);

        // The robot spins around its center but the turret is off-center, so its field position
        // moves around the robot as it turns. Rotate the offset by the heading to find it.
        double cos = Math.cos(heading);
        double sin = Math.sin(heading);
        turretX = x + turretOffsetForward * cos - turretOffsetLeft * sin;
        turretY = y + turretOffsetForward * sin + turretOffsetLeft * cos;

        boolean far = cell == Cell.FAR;
        targetX = far ? redFarCellX : redCloseCellX;
        targetY = far ? redFarCellY : redCloseCellY;
        if (alliance == Alliance.BLUE) {
            targetX = FIELD_SIZE - targetX;
        }

        double dx = targetX - turretX;
        double dy = targetY - turretY;
        distance = Math.hypot(dx, dy);
        turretAngle = wrapAngle(Math.atan2(dy, dx) - heading);
    }

    // Switches only once the robot is cellSwitchBand past the line, so sitting on it doesn't flicker
    private void updateCell(double y) {
        if (cell == null) {
            cell = y >= cellSwitchY ? Cell.FAR : Cell.CLOSE;
        } else if (cell == Cell.CLOSE && y > cellSwitchY + cellSwitchBand) {
            cell = Cell.FAR;
        } else if (cell == Cell.FAR && y < cellSwitchY - cellSwitchBand) {
            cell = Cell.CLOSE;
        }
    }

    /** Wraps an angle in radians to (-π, π]. */
    public static double wrapAngle(double angle) {
        double wrapped = angle % (2 * Math.PI);
        if (wrapped <= -Math.PI) wrapped += 2 * Math.PI;
        if (wrapped > Math.PI) wrapped -= 2 * Math.PI;
        return wrapped;
    }

    public void setAlliance(Alliance alliance) { this.alliance = alliance; }

    //---------------- Getters ----------------
    public Alliance getAlliance() { return alliance; }

    /** The Cell being aimed at, or null before the first update. */
    public Cell getCell() { return cell; }

    /**
     * Turret angle needed, relative to the robot's front: radians, counterclockwise (left) positive,
     * 0 = straight forward, in (-π, π].
     */
    public double getTurretAngle() { return turretAngle; }

    /** Flat distance from the turret pivot to the target Cell, inches. This is what ShooterData looks up. */
    public double getDistance() { return distance; }

    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
    public double getTurretX() { return turretX; }
    public double getTurretY() { return turretY; }
}
