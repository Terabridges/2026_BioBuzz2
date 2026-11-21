package org.firstinspires.ftc.teamcode.autonomous.framework;

import com.pedropathing.math.Pose;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.firstinspires.ftc.teamcode.autonomous.paths.FieldPoses;
import org.firstinspires.ftc.teamcode.autonomous.routines.DoNothingRoutine;

/**
 * Register each strategy once; Auto Select and Auto Path Test use this same list.
 * Entries are grouped by alliance and starting side using their AutoConfig fields.
 * An entry pairs its configuration with a factory that creates a fresh routine.
 * Set requiresRobot to true when the routine uses mechanism actions.
 * The competition menu initializes Robot if any entry needs it; path-test mode never does.
 * Only Do Nothing is registered for each alliance/side until real routes are implemented.
 */
public final class AutoCatalog {
    public static final class Entry {
        public final AutoConfig config;
        public final boolean requiresRobot;
        private final Function<AutoContext, AutoRoutine> factory;

        public Entry(AutoConfig config, boolean requiresRobot,
                     Function<AutoContext, AutoRoutine> factory) {
            this.config = Objects.requireNonNull(config);
            this.requiresRobot = requiresRobot;
            this.factory = Objects.requireNonNull(factory);
        }
        public AutoRoutine create(AutoContext context) {
            return Objects.requireNonNull(factory.apply(context), "Routine factory returned null");
        }
    }

    public static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
            doNothing(Alliance.BLUE, StartSide.AUDIENCE, FieldPoses.BLUE_AUDIENCE_START),
            doNothing(Alliance.BLUE, StartSide.REAR, FieldPoses.BLUE_REAR_START),
            doNothing(Alliance.RED, StartSide.AUDIENCE, FieldPoses.RED_AUDIENCE_START),
            doNothing(Alliance.RED, StartSide.REAR, FieldPoses.RED_REAR_START)
    ));

    private static Entry doNothing(Alliance alliance, StartSide side, Pose startPose) {
        return new Entry(new AutoConfig("Do Nothing", alliance, side, startPose, false),
                false, context -> new DoNothingRoutine());
    }
    private AutoCatalog() {}
}
