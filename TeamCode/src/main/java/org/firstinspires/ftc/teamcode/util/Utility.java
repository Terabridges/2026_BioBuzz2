package org.firstinspires.ftc.teamcode.util;

public class Utility {

    public Utility(){

    }

    public double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
