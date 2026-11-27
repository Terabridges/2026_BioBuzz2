package org.firstinspires.ftc.teamcode.utility.shooterUtility;

import com.bylazar.configurables.annotations.Configurable;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Velocity;

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
    public static double shiftDepthFactor = 7.5;
    public static double maxTargetShift = 7.0;

    //---------------- Shooting zone (red; blue is mirrored across x = 72) ----------------
    // Where the robot is allowed to auto-shoot from: the two rows of tiles (24 in. each) behind the Cell it's
    // aiming at, and five columns of tiles across starting at the left wall (a 2 x 5 rectangle). For the close
    // Cell that's y from the audience wall to 48; the far Cell's zone is the same thing from the far wall.
    // Blue's zone is moved over one column (x 24 to 144), which is the same as mirroring it.
    // Measured with the robot's center
    public static double zoneWidth = 120; // inches from the left wall (red)
    public static double zoneDepth = 48;  // inches from the wall behind the Cell

    //---------------- Robot ----------------
    // Where the turret pivot sits relative to the robot's center, in the robot's frame (inches).
    // Forward is toward the front of the robot, left is toward its left side.
    // Negative forward = toward the back. Robot is 416 mm long x 385 mm wide; the turret center is 189 mm
    // from the back, so 208 - 189 = 19 mm behind the center, and centered side to side.
    public static double turretOffsetForward = -19 / 25.4; // -0.748 in.
    public static double turretOffsetLeft = 0;

    //---------------- Shooting on the move ----------------
    // A ball leaves the robot with the robot's velocity added to it. So while the robot moves, aim at a
    // "virtual target": the Cell minus (robot velocity x flight time). The ball drifts by exactly that much in
    // the air and lands in the real Cell. The turret aims at the virtual target, and the RPM/hood tables use
    // the distance to it (driving toward the Cell gives the ball extra speed, so it acts like a closer shot).
    public static boolean leadEnabled = true;
    public static double leadScale = 1.0;   // multiplies the flight time. Shots land behind the way the robot is going: raise it
    public static double leadMinSpeed = 2;  // in/s. Slower than this counts as stopped: no lead
    public static int leadIterations = 5;   // flight time depends on distance and distance on flight time, so repeat a few times

    public enum Cell { CLOSE, FAR }

    private static final double FIELD_SIZE = 144;
    private final ShooterData shooterData = new ShooterData(); // for the flight-time table

    //---------------- State ----------------
    private Alliance alliance;
    private Cell cell = null; // picked on the first update from which half the robot is on

    //---------------- Results ----------------
    private double turretX, turretY;
    private double targetX, targetY;   // the real Cell
    private double aimX, aimY;         // where the turret aims: the Cell, or the virtual target while moving
    private double distance;           // turret pivot to the aim point
    private double realDistance;       // turret pivot to the real Cell
    private double flightTime;         // seconds used for the lead (0 when not leading)
    private boolean leading;
    private double turretAngle;
    private boolean inZone;

    //---------------- Constructor ----------------
    public TurretAim(Alliance alliance) {
        this.alliance = alliance;
    }

    //---------------- Methods ----------------
    /** Aims as if the robot were still (no lead). */
    public void update(Pose pose) {
        update(pose.x(), pose.y(), pose.heading());
    }

    /** Aims with the lead for the robot's velocity (field frame, inches per second). */
    public void update(Pose pose, Velocity velocity) {
        update(pose.x(), pose.y(), pose.heading(), velocity.vx, velocity.vy);
    }

    /** Aims as if the robot were still (no lead). */
    public void update(double x, double y, double heading) {
        update(x, y, heading, 0, 0);
    }

    /**
     * @param x       robot center x, inches
     * @param y       robot center y, inches
     * @param heading robot heading, radians, counterclockwise positive
     * @param vx      robot velocity along field x, inches per second
     * @param vy      robot velocity along field y, inches per second
     */
    public void update(double x, double y, double heading, double vx, double vy) {
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

        double lateralOffset = targetX - turretX;

        // Calculate the straight-line depth from the turret to the target plane
        double depthOffset = Math.abs(targetY - turretY);

        // Safety: prevent division by zero if the robot is sitting exactly on the target line
        if (depthOffset < 1.0) depthOffset = 1.0;

        // Calculate dynamic sensitivity (inversely proportional to distance)
        // E.g., if depth is 24 inches, sensitivity is 12/24 = 0.5
        // E.g., if depth is 96 inches, sensitivity is 12/96 = 0.125
        double dynamicSensitivity = shiftDepthFactor / depthOffset;

        // Multiply by your new dynamic sensitivity
        double shiftAmount = lateralOffset * dynamicSensitivity;

        // Clamp the shift so it never aims outside the physical walls
        shiftAmount = Math.max(-maxTargetShift, Math.min(maxTargetShift, shiftAmount));

        // Apply the shift to the target
        targetX += shiftAmount;

        realDistance = Math.hypot(targetX - turretX, targetY - turretY);
        updateLead(vx, vy);

        double dx = aimX - turretX;
        double dy = aimY - turretY;
        distance = Math.hypot(dx, dy);
        turretAngle = wrapAngle(Math.atan2(dy, dx) - heading);

        // Zone: flip the robot's position so one check works for both alliances and both Cells
        double fromSideWall = alliance == Alliance.BLUE ? FIELD_SIZE - x : x;
        double fromBackWall = far ? FIELD_SIZE - y : y;
        inZone = fromSideWall <= zoneWidth && fromBackWall <= zoneDepth;
    }

    // Picks the aim point. Uses the robot center's velocity for the turret: the pivot is only 0.75 in. off
    // center, so the extra speed it gets from the robot turning is too small to matter
    private void updateLead(double vx, double vy) {
        aimX = targetX;
        aimY = targetY;
        flightTime = 0;
        leading = leadEnabled && Math.hypot(vx, vy) > leadMinSpeed;
        if (!leading) return;

        double aimDistance = realDistance;
        for (int i = 0; i < Math.max(1, leadIterations); i++) {
            double t = shooterData.getShotTimeVal(aimDistance);
            if (t == shooterData.INVALID) t = 0; // off the table: don't lead
            flightTime = t * leadScale;
            aimX = targetX - vx * flightTime;
            aimY = targetY - vy * flightTime;
            aimDistance = Math.hypot(aimX - turretX, aimY - turretY);
        }
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

    /**
     * Flat distance from the turret pivot to the aim point, inches. This is what ShooterData looks up. Same as
     * {@link #getRealDistance()} unless the shot is being led.
     */
    public double getDistance() { return distance; }

    /** Flat distance from the turret pivot to the real Cell, inches, ignoring the lead. */
    public double getRealDistance() { return realDistance; }

    /** True when the aim is moved for the robot's velocity this loop. */
    public boolean isLeading() { return leading; }

    /** Flight time used for the lead, seconds (already times leadScale), or 0 when not leading. */
    public double getFlightTime() { return flightTime; }

    /** How far the aim point is moved from the real Cell, inches. */
    public double getLeadOffset() { return Math.hypot(aimX - targetX, aimY - targetY); }

    public double getAimX() { return aimX; }
    public double getAimY() { return aimY; }

    /** True when the robot's center is in the shooting zone for the Cell being aimed at. */
    public boolean isInZone() { return inZone; }

    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
    public double getTurretX() { return turretX; }
    public double getTurretY() { return turretY; }
}
