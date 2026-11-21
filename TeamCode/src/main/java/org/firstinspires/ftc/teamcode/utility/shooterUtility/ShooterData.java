package org.firstinspires.ftc.teamcode.utility.shooterUtility;

import com.arcrobotics.ftclib.util.InterpLUT;


public class ShooterData {

    //---------------- Range ----------------
    public final double MIN_DISTANCE = 0;    // inches
    public final double MAX_DISTANCE = 1000; // inches
    public final double INVALID = -2;

    // End rows repeat the closest measured row, so outside the measured range (12.8–87.5 in.) the
    // tables hold that row's values instead of bending toward made-up numbers at 0 and 1000
    private final double MIN_RPM = 2070;
    private final double MAX_RPM = 2520;
    private final double MIN_ANGLE = 0.400;
    private final double MAX_ANGLE = 0.820;
    // Same idea for the shot-time table (measured 18–80 in.)
    private final double MIN_SHOT_TIME = 0.50;
    private final double MAX_SHOT_TIME = 0.85;

    //---------------- Tables ----------------
    private final InterpLUT rpmLUT = new InterpLUT();
    private final InterpLUT angleLUT = new InterpLUT(); // hood servo position, 0.39–0.82
    private final InterpLUT shotTimeLUT = new InterpLUT(); // seconds the ball is in the air

    public ShooterData() {
        addRPMData();
        addAngleData();
        addShotTimeData();
        rpmLUT.createLUT();
        angleLUT.createLUT();
        shotTimeLUT.createLUT();
    }

    //---------------- Data ----------------
    // From the Shot Data Collector, 2026-11-17, distance from odometry. 12.8–48.5 in. are from
    // shotData_20261117_185320.csv (straight back from the close Cell); 54.6–87.5 in. are from
    // shotData_20261117_191338.csv (from the corners). The comment on each row is the robot pose it was saved at.
    // Distances must strictly increase, and no distance can appear twice
    private void addRPMData() {
        rpmLUT.add(MIN_DISTANCE, MIN_RPM);
        rpmLUT.add(12.8, 2070);  // (61.1, 44.2) 84.1 deg
        rpmLUT.add(20.6, 2080);  // (60.4, 36.4) 83.2 deg
        rpmLUT.add(28.8, 2120);  // (59.5, 28.2) 85.0 deg
        rpmLUT.add(36.4, 2140);  // (59.0, 20.6) 85.6 deg
        rpmLUT.add(41.1, 2190);  // (58.7, 15.9) 87.1 deg
        rpmLUT.add(48.5, 2230);  // (56.7, 8.5) 88.0 deg
        rpmLUT.add(54.6, 2250);  // (21.7, 17.5) 55.6 deg
        rpmLUT.add(66.3, 2320);  // (11.3, 11.5) 61.9 deg
        rpmLUT.add(68.3, 2370);  // (111.7, 13.5) 105.8 deg
        rpmLUT.add(87.5, 2520);  // (131.3, 7.6) 111.5 deg
        rpmLUT.add(MAX_DISTANCE, MAX_RPM);
    }

    private void addAngleData() {
        angleLUT.add(MIN_DISTANCE, MIN_ANGLE);
        angleLUT.add(12.8, 0.400);
        angleLUT.add(20.6, 0.470);
        angleLUT.add(28.8, 0.580);
        angleLUT.add(36.4, 0.700);
        angleLUT.add(41.1, 0.750);
        angleLUT.add(48.5, 0.760);
        angleLUT.add(54.6, 0.770);
        angleLUT.add(66.3, 0.790);
        angleLUT.add(68.3, 0.820);
        angleLUT.add(87.5, 0.820);
        angleLUT.add(MAX_DISTANCE, MAX_ANGLE);
    }

    // Flight time: from leaving the flywheel to reaching the Cell opening. Measured 2026-11-19 from slow-motion
    // video, shooting with these RPM/hood tables. Used to lead shots while the robot moves (TurretAim)
    private void addShotTimeData() {
        shotTimeLUT.add(MIN_DISTANCE, MIN_SHOT_TIME);
        shotTimeLUT.add(18, 0.50);
        shotTimeLUT.add(27, 0.50);
        shotTimeLUT.add(38, 0.65);
        shotTimeLUT.add(50, 0.70);
        shotTimeLUT.add(66, 0.80);
        shotTimeLUT.add(80, 0.85);
        shotTimeLUT.add(MAX_DISTANCE, MAX_SHOT_TIME);
    }

    //---------------- Lookups ----------------
    public double getRPMVal(double distance) {
        if (!inRange(distance)) return INVALID;
        return rpmLUT.get(distance);
    }

    public double getAngleVal(double distance) {
        if (!inRange(distance)) return INVALID;
        return angleLUT.get(distance);
    }

    /** Seconds the ball is in the air at this distance, or INVALID outside the table. */
    public double getShotTimeVal(double distance) {
        if (!inRange(distance)) return INVALID;
        return shotTimeLUT.get(distance);
    }

    private boolean inRange(double distance) {
        return distance > MIN_DISTANCE && distance < MAX_DISTANCE;
    }
}
