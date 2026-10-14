package org.firstinspires.ftc.teamcode.autonomous.framework;

import com.pedropathing.math.Pose;
import java.util.Objects;

/**
 * Settings selected before Start: currently the display name and starting pose.
 * This is configuration data; sequence logic belongs in the routine and path
 * geometry belongs in a path class. Add alliance/start choices as they become known.
 */
public final class AutoConfig {
    public final String name;
    public final Pose startPose;

    public AutoConfig(String name, Pose startPose) {
        this.name = Objects.requireNonNull(name);
        this.startPose = Objects.requireNonNull(startPose);
    }
}
