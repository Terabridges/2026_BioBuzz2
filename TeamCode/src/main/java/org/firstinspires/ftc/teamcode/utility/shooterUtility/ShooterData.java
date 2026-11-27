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
        rpmLUT.add(22.6, 2030);
        rpmLUT.add(30.0, 2070);
        rpmLUT.add(37.3, 2080);
        rpmLUT.add(41.7, 2080);
        rpmLUT.add(48.5, 2150);
        rpmLUT.add(51.8, 2150);
        rpmLUT.add(65.0, 2270);
        rpmLUT.add(87.5, 2520);
        rpmLUT.add(MAX_DISTANCE, MAX_RPM);
    }

    private void addAngleData() {
        angleLUT.add(MIN_DISTANCE, MIN_ANGLE);
        angleLUT.add(22.6, 0.490);
        angleLUT.add(30.0, 0.610);
        angleLUT.add(37.3, 0.680);
        angleLUT.add(41.7, 0.710);
        angleLUT.add(48.5, 0.780);
        angleLUT.add(51.8, 0.780);
        angleLUT.add(65.0, 0.800);
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
