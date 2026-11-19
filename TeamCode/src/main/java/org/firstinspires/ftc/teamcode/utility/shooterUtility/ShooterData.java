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

    //---------------- Tables ----------------
    private final InterpLUT rpmLUT = new InterpLUT();
    private final InterpLUT angleLUT = new InterpLUT(); // hood servo position, 0.39–0.82
    private final InterpLUT shotTimeLUT = new InterpLUT();

    public ShooterData() {
        addRPMData();
        addAngleData();
        rpmLUT.createLUT();
        angleLUT.createLUT();
        addShotTimeData();
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
        rpmLUT.add(48.5, 2270);  // (56.7, 8.5) 88.0 deg
        rpmLUT.add(54.6, 2250);  // (21.7, 17.5) 55.6 deg
        rpmLUT.add(66.3, 2310);  // (11.3, 11.5) 61.9 deg
        rpmLUT.add(68.3, 2370);  // (111.7, 13.5) 105.8 deg
        rpmLUT.add(87.5, 2520);  // (131.3, 7.6) 111.5 deg
        rpmLUT.add(MAX_DISTANCE, MAX_RPM);
    }

    private void addAngleData() {
        angleLUT.add(MIN_DISTANCE, MIN_ANGLE);
        angleLUT.add(12.8, 0.400);
        angleLUT.add(20.6, 0.460);
        angleLUT.add(28.8, 0.580);
        angleLUT.add(36.4, 0.700);
        angleLUT.add(41.1, 0.750);
        angleLUT.add(48.5, 0.810);
        angleLUT.add(54.6, 0.760);
        angleLUT.add(66.3, 0.780);
        angleLUT.add(68.3, 0.820);
        angleLUT.add(87.5, 0.820);
        angleLUT.add(MAX_DISTANCE, MAX_ANGLE);
    }

    private void addShotTimeData() {
        // Provisional DECODE flight times (seconds), originally credited to TurtleWalkers.
        // Replace with BIOBUZZ measurements at the CELL opening using this robot's shots.
        shotTimeLUT.add(0.0, 0.4);
        shotTimeLUT.add(42.5, 0.35);
        shotTimeLUT.add(55.0, 0.35);
        shotTimeLUT.add(66.7, 0.30);
        shotTimeLUT.add(81.9, 0.50);
        shotTimeLUT.add(95.7, 0.70);
        shotTimeLUT.add(101.9, 0.70);
        shotTimeLUT.add(116.6, 0.70);
        shotTimeLUT.add(136.6, 0.80);
        shotTimeLUT.add(3000.0, 1.0);
    }

    /** Provisional flight time in seconds for the compensated horizontal range in inches. */
    public double getShotTimeVal(double distance) {
        if (!Double.isFinite(distance)) return Double.NaN;
        return shotTimeLUT.get(Math.max(0.0, Math.min(3000.0, distance)));
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

    private boolean inRange(double distance) {
        return distance > MIN_DISTANCE && distance < MAX_DISTANCE;
    }
}
