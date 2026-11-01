package org.firstinspires.ftc.teamcode.subsystems.drive;

/** Manual commands: positive forward, right, and clockwise turn, for either backend. */
public interface Drive {
    void robotCentric(double forward, double right, double turn);
    void fieldCentric(double forward, double right, double turn);
}
