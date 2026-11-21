package org.firstinspires.ftc.teamcode.utility.shooterUtility;

import com.arcrobotics.ftclib.util.InterpLUT;


public class ShooterData {

    //---------------- Range ----------------
    public final double MIN_DISTANCE = 0;    // inches
    public final double MAX_DISTANCE = 1000; // inches
    public final double INVALID = -2;

    private final double MIN_RPM = 2070;
    private final double MAX_RPM = 2520;
    private final double MIN_ANGLE = 0.400;
    private final double MAX_ANGLE = 0.820;
    private final double MIN_SHOT_TIME = 0.50;
    private final double MAX_SHOT_TIME = 0.85;

    //---------------- Tables ----------------
    private final InterpLUT rpmLUT = new InterpLUT();
    private final InterpLUT angleLUT = new InterpLUT();
    private final InterpLUT shotTimeLUT = new InterpLUT();

    public ShooterData() {
        addRPMData();
        addAngleData();
        addShotTimeData();
        rpmLUT.createLUT();
        angleLUT.createLUT();
        shotTimeLUT.createLUT();
    }

    //---------------- Data ----------------
    private void addRPMData() {
        rpmLUT.add(MIN_DISTANCE, MIN_RPM);
        rpmLUT.add(12.8, 2070);
        rpmLUT.add(20.6, 2080);
        rpmLUT.add(28.8, 2120);
        rpmLUT.add(36.4, 2140);
        rpmLUT.add(41.1, 2190);
        rpmLUT.add(48.5, 2230);
        rpmLUT.add(54.6, 2250);
        rpmLUT.add(66.3, 2320);
        rpmLUT.add(68.3, 2370);
        rpmLUT.add(87.5, 2520);
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

    public double getShotTimeVal(double distance) {
        if (!inRange(distance)) return INVALID;
        return shotTimeLUT.get(distance);
    }

    private boolean inRange(double distance) {
        return distance > MIN_DISTANCE && distance < MAX_DISTANCE;
    }
}
