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


    private final double turretDefaultVel = 0.5; // servo units per second, 0.5 = full travel in 2 sec
    private final double turretManualVel = 0.3; // positive = counterclockwise (left)

    private final double turretRangeDeg = 480;
    private final double turretStartAngleDeg = 180;
    private final double turretDirection = 1;
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
    public void moveTurret(double pos, double vel){
        turretTargetPos = clampTurret(pos);
        turretVel = Math.abs(vel);
    }

    public void moveTurret(double pos){
        moveTurret(pos, turretDefaultVel);
    }

    public void setTurretVel(double vel){
        if (vel == 0){
            turretTargetPos = turretCurrentPos;
        } else {
            moveTurret(vel > 0 ? turretMaxPos : turretMinPos, vel);
        }
    }

    public void turnTurretLeft(){
        setTurretVel(turretManualVel);
    }

    public void turnTurretRight(){
        setTurretVel(-turretManualVel);
    }

    public void stopTurret(){
        setTurretVel(0);
    }

    public void aimAt(double angleRad){
        moveTurret(positionFor(angleRad, turretCurrentPos));
    }

    public double positionFor(double angleRad, double fromPos){
        double fromStartDeg = Math.toDegrees(TurretAim.wrapAngle(angleRad - Math.toRadians(turretStartAngleDeg)));
        double base = turretStartPos + turretDirection * fromStartDeg / turretRangeDeg;

        double best = Double.NaN;
        for (double pos : new double[]{base - fullTurnPos, base, base + fullTurnPos}) {
            if (clampTurret(pos) != pos) continue; // out of range
            if (Double.isNaN(best) || Math.abs(pos - fromPos) < Math.abs(best - fromPos)) best = pos;
        }
        return Double.isNaN(best) ? clampTurret(base) : best;
    }

    public double getTurretPos(){return turretCurrentPos;}

    public double getTurretTargetPos(){return turretTargetPos;}

    public boolean isTurretAtTarget(){
        return turretCurrentPos == turretTargetPos;
    }

    public boolean isTurretNearTarget(double tolDeg){
        return Math.abs(turretTargetPos - turretCurrentPos) * turretRangeDeg <= tolDeg;
    }

    public double getRightTurretOffset(){return rightTurretOffset;}

    //---------------- Interface Methods ----------------
    @Override
    public void stop(){
        stopTurret();
    }

    @Override
    public void init(){
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
            writeTurret(Math.abs(error) <= maxStep ? turretTargetPos : turretCurrentPos + Math.signum(error) * maxStep);
        }
    }

}
