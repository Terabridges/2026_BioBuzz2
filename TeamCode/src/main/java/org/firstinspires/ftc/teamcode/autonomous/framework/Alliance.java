package org.firstinspires.ftc.teamcode.autonomous.framework;

/** Alliance selected before Start; path classes use it to choose the correct field poses. */
public enum Alliance {
    BLUE("Blue Alliance"), RED("Red Alliance");

    public final String label;

    Alliance(String label) { this.label = label; }
}