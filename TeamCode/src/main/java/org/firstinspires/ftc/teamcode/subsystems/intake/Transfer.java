package org.firstinspires.ftc.teamcode.subsystems.intake;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.utility.Utility;

public class Transfer implements Subsystem {

    //---------------- Hardware ----------------
    private DcMotorEx transfer;
    private Servo blocker;
    //TODO figure out which distance we are using and implement them

    //---------------- Software ----------------
    private final Utility util = new Utility();

    private double transferPow = 0.0;
    private double blockerPos = 0.0;
    private boolean blockerChanged = false;

    private final double blockerMin = 0.52;
    private final double blockerMax = 0.66;
    private final double blockerClosedPos = 0.52;
    private final double blockerOpenPos = 0.62;
    private final double blockerStart = blockerClosedPos;
    private boolean blockerOpen = false;

    private final double transferDefaultPow = 1.0;

    //---------------- Constructor ----------------
    public Transfer(HardwareMap map) {
        transfer = map.get(DcMotorEx.class, "transfer");
        transfer.setDirection(DcMotorSimple.Direction.FORWARD);
        transfer.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        transfer.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        blocker = map.get(Servo.class, "blocker");
    }

    //---------------- Core Methods ----------------
    private void setTransfer(double pow){
        transfer.setPower(pow);
    }

    private void setBlocker(double pos){
        blocker.setPosition(util.clamp(pos, blockerMin, blockerMax));
    }

    //---------------- Methods ----------------
    public void setTransferPow(double pow){
        transferPow = pow;
    }

    public void setTransferIn(double pow){
        transferPow = Math.abs(pow);
    }

    public void setTransferOut(double pow){
        transferPow = -Math.abs(pow);
    }

    public void setTransferIn(){
        setTransferIn(transferDefaultPow);
    }

    public void setTransferOut(){
        setTransferOut(transferDefaultPow);
    }

    public void setTransferZero(){
        transferPow = 0.0;
    }

    public void setBlockerPos(double pos){
        blockerChanged = true;
        blockerPos = pos;
    }

    public void openBlocker(){
        blockerOpen = true;
        setBlockerPos(blockerOpenPos);
    }

    public void closeBlocker(){
        blockerOpen = false;
        setBlockerPos(blockerClosedPos);
    }

    public void toggleBlocker(){
        if (blockerOpen) closeBlocker();
        else openBlocker();
    }

    public boolean isBlockerOpen(){return blockerOpen;}

    //---------------- Interface Methods ----------------
    @Override
    public void init(){
        closeBlocker();
    }

    @Override
    public void update(){
        setTransfer(transferPow);

        if (blockerChanged){
            setBlocker(blockerPos);
            blockerChanged = false;
        }
    }
}
