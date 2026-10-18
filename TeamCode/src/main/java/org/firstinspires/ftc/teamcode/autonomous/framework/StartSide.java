package org.firstinspires.ftc.teamcode.autonomous.framework;

/**
 * Descriptive starting regions along your alliance wall, relative to your own
 * Garden and Loading Zone. These are team labels, not official starting-zone names.
 */
public enum StartSide {
    GARDEN("Garden Side"), LOADING("Loading Side");

    public final String label;

    StartSide(String label) { this.label = label; }
}
