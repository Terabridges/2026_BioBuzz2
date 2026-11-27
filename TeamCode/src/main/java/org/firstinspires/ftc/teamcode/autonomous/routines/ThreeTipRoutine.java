package org.firstinspires.ftc.teamcode.autonomous.routines;

import android.annotation.SuppressLint;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.sfdev.assembly.state.StateMachine;
import com.sfdev.assembly.state.StateMachineBuilder;
import org.firstinspires.ftc.teamcode.actions.Action;
import org.firstinspires.ftc.teamcode.actions.CollectAction;
import org.firstinspires.ftc.teamcode.actions.ShootAction;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoContext;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoRoutine;
import org.firstinspires.ftc.teamcode.autonomous.framework.StartSide;
import org.firstinspires.ftc.teamcode.autonomous.paths.FieldPoses;
import org.firstinspires.ftc.teamcode.autonomous.paths.StallProtectedPathAction;
import org.firstinspires.ftc.teamcode.autonomous.paths.ThreeTipPaths;
import org.firstinspires.ftc.teamcode.utility.shooterUtility.TurretAim;

/** Unified Three Tip route handling both Close (Audience, 4 paths) and Far (Rear, 5 paths) start sides. */
public final class ThreeTipRoutine implements AutoRoutine {
    private static final double GARDEN_WAIT_SECONDS = 0.75;
    private static final double GARDEN_SHIMMY_SECONDS = 1.5;
    private static final double FLOWER_WAIT_SECONDS = 1.0;
    private static final double FLOWER_SHIMMY_SECONDS = 1.25;
    private static final double SHIMMY_STEP_SECONDS = 0.25;
    private static final double GARDEN_SHIMMY_INCHES = 2.0;
    private static final double FLOWER_SHIMMY_INCHES = 1.0;

    private static final double HIVE_WAIT_TIMEOUT_SECONDS = 5.0;
    private static final double ALLIANCE_PARTNER_WAIT_SECONDS = 4.0;

    private enum State {
        WAIT_FOR_HIVE_PRELOAD("Waiting for hive tip (Preload)"),
        SHOOT_PRELOAD("Shoot preload"),

        // Far-only first leg (Start -> Rear Flower)
        TO_FAR_REAR_FLOWER("Far 1/5 Start to Rear Flower"),
        SHOOT_REAR_FLOWER("Shoot Rear Flower"),

        // Shared Garden leg (Start -> Garden on Close; Rear Flower -> Garden on Far)
        TO_GARDEN("Drive to Garden"),
        WAIT_FOR_ALLIANCE_AND_HIVE_GARDEN("Waiting for alliance & hive tip (Garden)"),
        SHOOT_GARDEN("Shoot Garden"),

        // Shared Side Flower leg (Garden -> Side Flower -> Shoot -> Park)
        TURN_TO_SIDE_FLOWER("Turn toward Side Flower"),
        TO_SIDE_FLOWER("Drive to Side Flower"),
        DRIVE_SIDE_FLOWER_TO_SHOOT("Drive Side Flower to Shoot"),
        SHOOT_SIDE_FLOWER("Shoot Side Flower"),

        TO_PARK("Drive to Park"),
        SHOT_TIMEOUT("Shot timed out"),
        DONE("");

        final String label;
        State(String label) { this.label = label; }
    }

    private final AutoContext context;
    private final boolean pathTest;
    private final boolean waitForHive;
    private final boolean isClose;

    private final TurretAim.Cell preloadCell;
    private final TurretAim.Cell farRearFlowerCell;
    private final TurretAim.Cell gardenCell;
    private final TurretAim.Cell sideFlowerCell;

    private final StallProtectedPathAction farRearFlowerApproach;
    private final StallProtectedPathAction gardenApproach;
    private final StallProtectedPathAction sideFlowerApproach;
    private final Path sideFlowerToShootPath;
    private final Path parkPath;

    private final Action shoot;
    private final Action gardenCollect;
    private final Action flowerCollect;

    private final ElapsedTime collectionTimer = new ElapsedTime();
    private final ElapsedTime waitTimer = new ElapsedTime();
    private final ElapsedTime allianceTimer = new ElapsedTime();

    private Pose shimmyCenter;
    private int lastShimmyStep;
    private final StateMachine machine;
    private boolean previousAdvance;
    private boolean advancePressed;
    private boolean waitingForAdvance;
    private Result result = Result.READY;

    private boolean isHiveTipped = false;

    public ThreeTipRoutine(AutoContext context, boolean waitForHive) {
        this.context = context;
        this.waitForHive = waitForHive;
        this.pathTest = context.mode == AutoContext.Mode.PATH_TEST;
        this.isClose = context.config.startSide == StartSide.AUDIENCE;

        if (isClose) {
            preloadCell = TurretAim.Cell.CLOSE;
            farRearFlowerCell = null;
        } else {
            preloadCell = TurretAim.Cell.FAR;
            farRearFlowerCell = TurretAim.Cell.CLOSE;
        }
        gardenCell = TurretAim.Cell.FAR;
        sideFlowerCell = TurretAim.Cell.FAR;

        ThreeTipPaths paths = new ThreeTipPaths(context.config.startPose);

        if (isClose) {
            farRearFlowerApproach = null;
            gardenApproach = new StallProtectedPathAction(context.follower, paths.closeStartToGarden);
            sideFlowerApproach = new StallProtectedPathAction(context.follower, paths.closeGardenToSideFlower);
            sideFlowerToShootPath = paths.closeSideFlowerToShoot;
            parkPath = paths.closeShootToPark;
        } else {
            farRearFlowerApproach = new StallProtectedPathAction(context.follower, paths.farStartToRearFlower);
            gardenApproach = new StallProtectedPathAction(context.follower, paths.farRearFlowerToGarden);
            sideFlowerApproach = new StallProtectedPathAction(context.follower, paths.farGardenToSideFlower);
            sideFlowerToShootPath = paths.farSideFlowerToShoot;
            parkPath = paths.farShootToPark;
        }

        shoot = context.mechanismAction(robot -> new ShootAction(robot.outtake, robot.intake, () -> true, 5.0));
        gardenCollect = context.mechanismAction(robot ->
                new CollectAction(robot.intake, 4, GARDEN_WAIT_SECONDS + GARDEN_SHIMMY_SECONDS, 0.15));
        flowerCollect = context.mechanismAction(robot ->
                new CollectAction(robot.intake, 4, FLOWER_WAIT_SECONDS + FLOWER_SHIMMY_SECONDS, 0.15));

        machine = buildMachine();
    }

    private StateMachine buildMachine() {
        Follower follower = context.follower;
        Pose turnTarget = FieldPoses.ThreeTip.AUDIENCE_FLOWER_FACING_WALL;

        return new StateMachineBuilder()
                // --- PRELOAD ---
                .state(State.WAIT_FOR_HIVE_PRELOAD)
                // If close, bypass the hive condition check entirely
                .transition(() -> isClose || hiveConditionMet(), State.SHOOT_PRELOAD, () -> {
                    context.runMechanism(robot -> robot.outtake.enableShooting(context.config.alliance, preloadCell));
                    shoot.start();
                })
                .state(State.SHOOT_PRELOAD)
                .loop(shoot::update)
                .transition(this::shotTimedOut, State.SHOT_TIMEOUT)
                .transition(() -> canAdvanceAfter(shotComplete()),
                        isClose ? State.TO_GARDEN : State.TO_FAR_REAR_FLOWER,
                        () -> {
                            context.runMechanism(robot -> {
                                robot.intake.collect();
                                robot.outtake.setTargetCell(isClose ? gardenCell : farRearFlowerCell);
                            });
                            if (isClose) {
                                gardenApproach.start();
                            } else {
                                farRearFlowerApproach.start();
                            }
                        })

                // --- FAR-ONLY LEG: REAR FLOWER ---
                .state(State.TO_FAR_REAR_FLOWER)
                .loop(() -> { if (!isClose) farRearFlowerApproach.update(); })
                .transition(() -> !isClose && canLeaveIntakePath(
                                farRearFlowerApproach.result() == Action.Result.SUCCEEDED
                                        || farRearFlowerApproach.result() == Action.Result.TIMED_OUT,
                                flowerCollect, FLOWER_WAIT_SECONDS, FLOWER_SHIMMY_INCHES),
                        State.SHOOT_REAR_FLOWER, shoot::start)
                .state(State.SHOOT_REAR_FLOWER)
                .loop(shoot::update)
                .transition(this::shotTimedOut, State.SHOT_TIMEOUT)
                .transition(() -> canAdvanceAfter(shotComplete()), State.TO_GARDEN, () -> {
                    context.runMechanism(robot -> {
                        robot.intake.collect();
                        robot.outtake.setTargetCell(gardenCell);
                    });
                    gardenApproach.start();
                })

                // --- SHARED LEG 1: GARDEN (COMBINED WAIT) ---
                .state(State.TO_GARDEN)
                .loop(gardenApproach::update)
                .transition(() -> canLeaveIntakePath(
                                gardenApproach.result() == Action.Result.SUCCEEDED
                                        || gardenApproach.result() == Action.Result.TIMED_OUT,
                                gardenCollect, GARDEN_WAIT_SECONDS, GARDEN_SHIMMY_INCHES),
                        State.WAIT_FOR_ALLIANCE_AND_HIVE_GARDEN, () -> {
                            allianceTimer.reset();
                            waitTimer.reset();
                        })
                .state(State.WAIT_FOR_ALLIANCE_AND_HIVE_GARDEN)
                // If Far (!isClose) bypass instantly. Otherwise require Alliance + Hive timers.
                .transition(() -> !isClose || ((allianceTimer.seconds() > ALLIANCE_PARTNER_WAIT_SECONDS || (pathTest && advancePressed))
                                && hiveConditionMet()),
                        State.SHOOT_GARDEN, shoot::start)
                .state(State.SHOOT_GARDEN)
                .loop(shoot::update)
                .transition(this::shotTimedOut, State.SHOT_TIMEOUT)
                .transition(() -> canAdvanceAfter(shotComplete()), State.TURN_TO_SIDE_FLOWER, () ->
                        follower.hold(new Pose(follower.pose().x(), follower.pose().y(), turnTarget.heading())))

                // --- SHARED LEG 2: SIDE FLOWER -> SHOOT -> PARK ---
                .state(State.TURN_TO_SIDE_FLOWER)
                .transition(() -> canAdvanceAfter(atTurnTarget(follower.pose(), turnTarget)),
                        State.TO_SIDE_FLOWER, () -> {
                            context.runMechanism(robot -> {
                                robot.intake.ramp.setRampDown();
                                robot.intake.collect();
                                robot.outtake.setTargetCell(sideFlowerCell);
                            });
                            sideFlowerApproach.start();
                        })
                .state(State.TO_SIDE_FLOWER)
                .loop(sideFlowerApproach::update)
                .transition(() -> canLeaveIntakePath(
                                sideFlowerApproach.result() == Action.Result.SUCCEEDED
                                        || sideFlowerApproach.result() == Action.Result.TIMED_OUT,
                                flowerCollect, FLOWER_WAIT_SECONDS, FLOWER_SHIMMY_INCHES),
                        State.DRIVE_SIDE_FLOWER_TO_SHOOT, () -> follower.follow(sideFlowerToShootPath))
                .state(State.DRIVE_SIDE_FLOWER_TO_SHOOT)
                .transition(this::canAdvanceAfterDrive, State.SHOOT_SIDE_FLOWER, shoot::start)
                .state(State.SHOOT_SIDE_FLOWER)
                .loop(shoot::update)
                .transition(this::shotTimedOut, State.SHOT_TIMEOUT)
                .transition(() -> canAdvanceAfter(shotComplete()), State.TO_PARK, () -> follower.follow(parkPath))

                // --- PARK & END ---
                .state(State.TO_PARK)
                .transition(this::canAdvanceAfterDrive, State.DONE, () -> context.runMechanism(robot -> {
                    robot.intake.ramp.setRampUp();
                    robot.intake.ramp.update();
                }))
                .state(State.SHOT_TIMEOUT)
                .state(State.DONE)
                .build();
    }

    private boolean hiveConditionMet() {
        return !waitForHive || isHiveTipped || waitTimer.seconds() > HIVE_WAIT_TIMEOUT_SECONDS || (pathTest && advancePressed);
    }

    private boolean shotComplete() { return shoot.result() == Action.Result.SUCCEEDED; }
    private boolean shotTimedOut() { return shoot.result() == Action.Result.TIMED_OUT; }

    private boolean canLeaveIntakePath(boolean pathFinished, Action collect, double waitSeconds, double shimmyInches) {
        Follower follower = context.follower;
        if (pathTest) return canAdvanceAfter(pathFinished);
        if (shimmyCenter == null) {
            if (!pathFinished) return false;
            shimmyCenter = follower.pose();
            collectionTimer.reset();
            lastShimmyStep = -1;
            collect.start();
        }
        collect.update();
        if (collect.isFinished()) {
            if (lastShimmyStep >= 0) follower.stop();
            shimmyCenter = null;
            return true;
        }

        double shimmyTime = collectionTimer.seconds() - waitSeconds;
        if (shimmyTime >= 0) {
            int step = (int) (shimmyTime / SHIMMY_STEP_SECONDS) % 4;
            if (step != lastShimmyStep) {
                double forward = step == 0 ? -shimmyInches : 0;
                double left = step == 2 ? shimmyInches : 0;
                double h = shimmyCenter.heading();
                follower.hold(new Pose(
                        shimmyCenter.x() + forward * Math.cos(h) - left * Math.sin(h),
                        shimmyCenter.y() + forward * Math.sin(h) + left * Math.cos(h),
                        h
                ));
                lastShimmyStep = step;
            }
        }
        return false;
    }

    private boolean canAdvanceAfterDrive() {
        return canAdvanceAfter(!context.follower.isBusy());
    }

    private boolean canAdvanceAfter(boolean movementFinished) {
        if (!pathTest) return movementFinished;
        if (waitingForAdvance) {
            if (!advancePressed) return false;
            waitingForAdvance = false;
            return true;
        }
        if (movementFinished) {
            context.follower.stop();
            waitingForAdvance = true;
        }
        return false;
    }

    private static boolean atTurnTarget(Pose current, Pose target) {
        double error = Math.IEEEremainder(current.heading() - target.heading(), 2 * Math.PI);
        return Math.abs(error) <= Math.toRadians(5);
    }

    @Override public void start() {
        previousAdvance = context.pathTestAdvanceDown();
        advancePressed = false;
        waitingForAdvance = false;
        shimmyCenter = null;
        result = Result.RUNNING;

        waitTimer.reset();
        machine.setState(State.WAIT_FOR_HIVE_PRELOAD);
        machine.start();
        machine.update();
    }

    @Override public void update() {
        if (result != Result.RUNNING) return;

        boolean pressed = context.pathTestAdvanceDown();
        advancePressed = pressed && !previousAdvance;
        previousAdvance = pressed;

        State current = (State) machine.getState();

        // Check HuskyLens for all states where we might be waiting for the hive
        if (waitForHive && (current == State.WAIT_FOR_HIVE_PRELOAD
                || current == State.WAIT_FOR_ALLIANCE_AND_HIVE_GARDEN)) {
            context.runMechanism(robot -> isHiveTipped = robot.vision.hasHuskyLensTag());
        }

        machine.update();
        if (machine.getState() == State.DONE) {
            machine.stop();
            result = Result.SUCCEEDED;
        } else if (machine.getState() == State.SHOT_TIMEOUT) {
            machine.stop();
            result = Result.TIMED_OUT;
        }
    }

    @Override public void stop() {
        machine.stop();
        shoot.stop();
        gardenCollect.stop();
        flowerCollect.stop();
        if (farRearFlowerApproach != null) farRearFlowerApproach.stop();
        if (gardenApproach != null) gardenApproach.stop();
        if (sideFlowerApproach != null) sideFlowerApproach.stop();
        if (!isFinished()) result = Result.CANCELLED;
    }

    @Override public Result result() { return result; }

    @SuppressLint("DefaultLocale")
    @Override public String state() {
        if (result == Result.TIMED_OUT) return ((State) machine.getState()).label;
        if (result != Result.RUNNING) return result.name();
        State current = (State) machine.getState();

        if (current == State.WAIT_FOR_HIVE_PRELOAD) {
            if (!waitForHive || isClose) return "Skipping hive wait";
            if (pathTest) return "Press A to skip hive wait";
            return String.format("%s (%.1fs elapsed)", current.label, waitTimer.seconds());
        }

        if (current == State.WAIT_FOR_ALLIANCE_AND_HIVE_GARDEN) {
            if (!isClose) return "Skipping alliance/hive wait";
            if (pathTest) return "Press A to advance | Waiting for alliance/hive";

            boolean allianceDone = allianceTimer.seconds() > ALLIANCE_PARTNER_WAIT_SECONDS;
            String allianceStatus = allianceDone ? "Clear" : String.format("Wait %.1fs", ALLIANCE_PARTNER_WAIT_SECONDS - allianceTimer.seconds());

            String hiveStatus = (!waitForHive) ? "Skip" : (isHiveTipped ? "Seen" : String.format("Wait %.1fs", HIVE_WAIT_TIMEOUT_SECONDS - waitTimer.seconds()));

            return String.format("Garden: Alliance [%s] | Hive [%s]", allianceStatus, hiveStatus);
        }

        if (shimmyCenter != null) {
            String location = current == State.TO_GARDEN ? "Garden" : "Flower";
            return (lastShimmyStep < 0 ? "Collecting at " : "Shimmy at ") + location;
        }

        if (waitingForAdvance) return "Press A to advance | Finished " + current.label;
        return "Running " + current.label;
    }
}