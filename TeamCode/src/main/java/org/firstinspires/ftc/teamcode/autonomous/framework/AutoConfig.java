package org.firstinspires.ftc.teamcode.autonomous.framework;

import com.pedropathing.math.Pose;
import java.util.Objects;

/**
 * Settings selected before Start: routine name, alliance, starting side, and starting pose.
 * This is configuration data; sequence logic belongs in the routine and path
 * geometry belongs in a path class. Alliance and side do not automatically transform
 * coordinates; each catalog entry must supply the correct measured starting pose.
 */
public final class AutoConfig {
    public final String name;
    public final Alliance alliance;
    public final StartSide startSide;
    public final Pose startPose;

    public AutoConfig(String name, Alliance alliance, StartSide startSide, Pose startPose) {
        this.name = Objects.requireNonNull(name);
        this.alliance = Objects.requireNonNull(alliance);
        this.startSide = Objects.requireNonNull(startSide);
        this.startPose = Objects.requireNonNull(startPose);
    }

    public String selectionLabel() {
        return alliance.label + " > " + startSide.label + " > " + name;
    }
}
