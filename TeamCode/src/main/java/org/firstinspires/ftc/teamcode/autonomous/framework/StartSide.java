package org.firstinspires.ftc.teamcode.autonomous.framework;

/**
 * Descriptive starting regions along your alliance wall, relative to your own
 * Garden and Loading Zone. These menu choices do not specify a measured robot pose.
 */
public enum StartSide {
    GARDEN("Garden Side"), LOADING("Loading Side");

    public final String label;

    StartSide(String label) { this.label = label; }
}
