package org.firstinspires.ftc.teamcode.utility.shooterUtility;

import com.arcrobotics.ftclib.util.InterpLUT;


public class ShooterData {

    //---------------- Range ----------------
    public final double MIN_DISTANCE = 0;    // inches
    public final double MAX_DISTANCE = 1000; // inches
    public final double INVALID = -2;

    // End rows repeat the closest measured row, so outside the measured range (17.9–55.2 in.) the
    // tables hold that row's values instead of bending toward made-up numbers at 0 and 1000
    private final double MIN_RPM = 1970;
    private final double MAX_RPM = 2190;
    private final double MIN_ANGLE = 0.430;
    private final double MAX_ANGLE = 0.820;

    //---------------- Tables ----------------
    private final InterpLUT rpmLUT = new InterpLUT();
    private final InterpLUT angleLUT = new InterpLUT(); // hood servo position, 0.39–0.82

    public ShooterData() {
        addRPMData();
        addAngleData();
        rpmLUT.createLUT();
        angleLUT.createLUT();
    }

    //---------------- Data ----------------
    // From the Shot Data Collector, 2026-11-10 (shotData_20261110_193002.csv), distance from odometry.
    // Distances must strictly increase, and no distance can appear twice
    private void addRPMData() {
        rpmLUT.add(MIN_DISTANCE, MIN_RPM);
        rpmLUT.add(17.9, 1970);
        rpmLUT.add(28.8, 2000);
        rpmLUT.add(34.7, 2060);
        rpmLUT.add(40.4, 2100);
        rpmLUT.add(49.1, 2130); // measured 2120, bumped +10
        rpmLUT.add(55.2, 2190);
        rpmLUT.add(MAX_DISTANCE, MAX_RPM);
    }

    private void addAngleData() {
        angleLUT.add(MIN_DISTANCE, MIN_ANGLE);
        angleLUT.add(17.9, 0.430);
        angleLUT.add(28.8, 0.560);
        angleLUT.add(34.7, 0.700);
        angleLUT.add(40.4, 0.780);
        angleLUT.add(49.1, 0.820);
        angleLUT.add(55.2, 0.820);
        angleLUT.add(MAX_DISTANCE, MAX_ANGLE);
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
