package org.firstinspires.ftc.teamcode.subsystems.outtake;

import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.utility.AbsoluteAnalogEncoder;
import org.firstinspires.ftc.teamcode.utility.Utility;
import org.firstinspires.ftc.teamcode.utility.shooterUtility.TurretAim;

public class Turret implements Subsystem {

    //---------------- Hardware ----------------
    private Servo turretLeft;
    private Servo turretRight;
    private AnalogInput turretAnalog;
    private AbsoluteAnalogEncoder turretEnc;
    private Utility util;

    //---------------- Software ----------------
    private final double rightTurretOffset = -0.008;
    private boolean negativeOffset;

    private final double turretPwmMinUs = 500; // are these the right limits?
    private final double turretPwmMaxUs = 2500;
    private final double turretMinPos = 0;
    private final double turretMaxPos = 1;
    private final double turretStartPos = 0.5;

    double dt;
    double maxStep;
    double error;

    // 0-1 is one full turn of the servo (360 deg), which turns the turret 480 deg through the 3:4 gears.
    // The servo takes the shortest way to a new position, so if the command gets half a turn (0.5)
    // ahead of where the servo really is, it goes the wrong way, across 0/1. Every move is stepped at
    // a velocity to keep the command just ahead of the servo
    // TODO measure how fast the turret really turns: no velocity here may be faster than that
    private final double turretDefaultVel = 0.5; // servo units per second, 0.5 = full travel in 2 sec
    // TODO verify that positive velocity is counterclockwise, flip the sign if the turret goes the wrong way

    private final double turretManualVel = 0.3; // positive = counterclockwise (left)

    // Aiming: degrees of turret over the full 0-1 servo range, and which way a higher position turns it
    private final double turretRangeDeg = 480;
    // Where the turret points at turretStartPos (0.5), from the robot's front: 180 = straight back
    private final double turretStartAngleDeg = 180;
    // TODO verify: +1 if a higher servo position turns the turret counterclockwise (left), -1 if clockwise
    private final double turretDirection = 1;
    // One full turret turn in servo units (0.75). Positions this far apart point the same way
    private final double fullTurnPos = 360 / turretRangeDeg;

    private final ElapsedTime loopTimer = new ElapsedTime();
    private double turretCurrentPos = turretStartPos;
    private double turretTargetPos = turretStartPos;
    private double turretVel = turretDefaultVel;

    //---------------- Constructor ----------------
    public Turret(HardwareMap map) {
        turretLeft = map.get(Servo.class, "turret_left");
        turretRight = map.get(Servo.class, "turret_right");
        turretAnalog = map.get(AnalogInput.class, "turret_left_analog");
        turretEnc = new AbsoluteAnalogEncoder(turretAnalog, 3.3, 0, 1);
        util = new Utility();
        PwmControl.PwmRange pwmRange = new PwmControl.PwmRange(turretPwmMinUs, turretPwmMaxUs);
        ((PwmControl) turretLeft).setPwmRange(pwmRange);
        ((PwmControl) turretRight).setPwmRange(pwmRange);

        negativeOffset = rightTurretOffset < 0;
    }

    //---------------- Core Methods ----------------
    private double clampTurret(double pos){
        return util.clamp(pos, (negativeOffset ? turretMinPos-rightTurretOffset : turretMinPos), (negativeOffset ? turretMaxPos : turretMaxPos-rightTurretOffset));
    }

    private void writeTurret(double pos){
        turretCurrentPos = clampTurret(pos);
        turretLeft.setPosition(turretCurrentPos);
        turretRight.setPosition(turretCurrentPos+rightTurretOffset);
    }

    //---------------- Methods ----------------
    // Moves toward pos a bit each update(), at vel servo units per second
    public void moveTurret(double pos, double vel){
        turretTargetPos = clampTurret(pos);
        turretVel = Math.abs(vel);
    }

    public void moveTurret(double pos){
        moveTurret(pos, turretDefaultVel);
    }

    // Keeps moving at vel servo units per second (sign = direction) until it hits a limit, 0 stops it where it is
    public void setTurretVel(double vel){
        if (vel == 0){
            turretTargetPos = turretCurrentPos;
        } else {
            moveTurret(vel > 0 ? turretMaxPos : turretMinPos, vel);
        }
    }

    // Counterclockwise
    public void turnTurretLeft(){
        setTurretVel(turretManualVel);
    }

    // Clockwise
    public void turnTurretRight(){
        setTurretVel(-turretManualVel);
    }

    public void stopTurret(){
        setTurretVel(0);
    }

    // Points the turret at an angle from the robot's front: radians, counterclockwise (left) positive.
    // Call it every loop while tracking. The move is still rate-limited by update(), so it can't wrap
    public void aimAt(double angleRad){
        moveTurret(positionFor(angleRad, turretCurrentPos));
    }

    // The servo position that points the turret at angleRad (from the robot's front). 0.5 points straight
    // back, so the angle is measured from there first. The range covers 480 deg, so within about 60 deg of
    // straight forward there are two positions (a full turn apart) that point the same way: take the one
    // closer to fromPos, so the turret doesn't swing all the way around when it doesn't have to
    public double positionFor(double angleRad, double fromPos){
        // Angle from the start direction (straight back), in -180..180
        double fromStartDeg = Math.toDegrees(TurretAim.wrapAngle(angleRad - Math.toRadians(turretStartAngleDeg)));
        double base = turretStartPos + turretDirection * fromStartDeg / turretRangeDeg;


        //Hey Mike if you are reading this I need help understanding this part...
        double best = Double.NaN;
        for (double pos : new double[]{base - fullTurnPos, base, base + fullTurnPos}) {
            if (clampTurret(pos) != pos) continue; // out of range
            if (Double.isNaN(best) || Math.abs(pos - fromPos) < Math.abs(best - fromPos)) best = pos;
        }
        // The usable range is wider than a full turn, so one always fits. Clamp just in case it doesn't
        return Double.isNaN(best) ? clampTurret(base) : best;
    }

    public double getTurretPos(){return turretCurrentPos;}

    public double getTurretTargetPos(){return turretTargetPos;}

    public boolean isTurretAtTarget(){
        return turretCurrentPos == turretTargetPos;
    }

    public double getRightTurretOffset(){return rightTurretOffset;}

    //---------------- Interface Methods ----------------
    @Override
    public void stop(){
        stopTurret();
    }

    @Override
    public void init(){
        // The one direct write: the code can't know where the turret really is at startup, so it
        // assumes the start position. If the turret was left near either end this can still wrap
        turretTargetPos = turretStartPos;
        writeTurret(turretStartPos);
        loopTimer.reset();
    }

    @Override
    public void update(){

        dt = loopTimer.seconds();
        loopTimer.reset();

        if (!isTurretAtTarget()){
            maxStep = turretVel * dt;
            error = turretTargetPos - turretCurrentPos;
            // Snap to the target on the last step so isTurretAtTarget() becomes exactly true
            writeTurret(Math.abs(error) <= maxStep ? turretTargetPos : turretCurrentPos + Math.signum(error) * maxStep);
        }
    }

}
