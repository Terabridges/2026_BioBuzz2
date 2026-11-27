package org.firstinspires.ftc.teamcode.autonomous.framework;

import com.pedropathing.math.Pose;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.firstinspires.ftc.teamcode.autonomous.paths.FieldPoses;
import org.firstinspires.ftc.teamcode.autonomous.routines.*;

/**
 - Register each strategy once; Auto Select and Auto Path Test use this same list.
 - Entries are grouped by alliance and starting side using their AutoConfig fields.
 - An entry pairs its configuration with a factory that creates a fresh routine.
 - Set requiresRobot to true when the routine uses mechanism actions.
 - Competition initializes Robot at Start when the selected entry needs it.
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
            // ---------------- BLUE ALLIANCE (Default kept for safety) ----------------
            doNothing(Alliance.BLUE, StartSide.AUDIENCE, FieldPoses.BLUE_AUDIENCE_START),
            doNothing(Alliance.BLUE, StartSide.REAR, FieldPoses.BLUE_REAR_START),

            // ---------------- RED AUDIENCE (CLOSE) ----------------
            doNothing(Alliance.RED, StartSide.AUDIENCE, FieldPoses.RED_AUDIENCE_START),

            new Entry(new AutoConfig("Preload Tip Close", Alliance.RED, StartSide.AUDIENCE,
                    FieldPoses.RED_AUDIENCE_START), true, ShootPreloadRoutine::new), //Preload, park?

            new Entry(new AutoConfig("Two Tip Close", Alliance.RED, StartSide.AUDIENCE,
                    FieldPoses.RED_AUDIENCE_START), true, context -> new TwoTipRoutine(context, false)), //Accommodate partner shooting their preload look down

            //TODO: Accommodate partner shooting
//            new Entry(new AutoConfig("Two Tip Close Partner Help", Alliance.RED, StartSide.AUDIENCE,
//                    FieldPoses.RED_AUDIENCE_START), true, context -> new TwoTipRoutine(context, false)), //Accommodate partner shooting their preload


            new Entry(new AutoConfig("3 Tip Close", Alliance.RED, StartSide.AUDIENCE,
                    FieldPoses.RED_AUDIENCE_START), true, context -> new ThreeTipRoutine(context, false)), //Preload, garden and wait, shoot flower shoot


            // ---------------- RED REAR (FAR) ----------------
            doNothing(Alliance.RED, StartSide.REAR, FieldPoses.RED_REAR_START),

            new Entry(new AutoConfig("Preload Tip Far", Alliance.RED, StartSide.REAR,
                    FieldPoses.RED_REAR_START), true, ShootPreloadRoutine::new), //Wait, preload

            new Entry(new AutoConfig("Two Tip Far Wait", Alliance.RED, StartSide.REAR,
                    FieldPoses.RED_REAR_START), true, context -> new TwoTipRoutine(context, true)), //Wait, preload and flower


            new Entry(new AutoConfig("3 Tip Far Wait", Alliance.RED, StartSide.REAR,
                    FieldPoses.RED_REAR_START), true, context -> new ThreeTipRoutine(context, true)) //Wait, preload and flower, drive other side, garden and side
    ));

    private static Entry doNothing(Alliance alliance, StartSide side, Pose startPose) {
        return new Entry(new AutoConfig("Do Nothing", alliance, side, startPose, false),
                false, context -> new DoNothingRoutine());
    }
    private AutoCatalog() {}
}