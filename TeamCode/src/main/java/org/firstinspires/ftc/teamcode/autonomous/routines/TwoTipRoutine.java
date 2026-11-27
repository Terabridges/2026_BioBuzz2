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
import org.firstinspires.ftc.teamcode.utility.shooterUtility.TurretAim;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoContext;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoRoutine;
import org.firstinspires.ftc.teamcode.autonomous.framework.StartSide;
import org.firstinspires.ftc.teamcode.autonomous.paths.FieldPoses;
import org.firstinspires.ftc.teamcode.autonomous.paths.StallProtectedPathAction;
import org.firstinspires.ftc.teamcode.autonomous.paths.TwoTipPaths;

/** Unified Two Tip route handling both Close (Audience) and Far (Rear) start sides. */
public final class TwoTipRoutine implements AutoRoutine {
    private static final double GARDEN_WAIT_SECONDS = 0.75;
    private static final double GARDEN_SHIMMY_SECONDS = 1.5;
    private static final double FLOWER_WAIT_SECONDS = 1.0;
    private static final double FLOWER_SHIMMY_SECONDS = 1.25;
    private static final double SHIMMY_STEP_SECONDS = 0.25;
    private static final double GARDEN_SHIMMY_INCHES = 2.0;
    private static final double FLOWER_SHIMMY_INCHES = 1.0;
    private static final double HIVE_WAIT_TIMEOUT_SECONDS = 5.0;

    private enum State {
        WAIT_FOR_HIVE("Waiting for hive tip"),
        SHOOT_PRELOAD("Shoot preload"),

        // Close Route Specific States
        TO_GARDEN("1/5 Start to Garden"),
        CROSS_FIELD("2/5 Garden to rear Flower lineup"),
        SHOOT_GARDEN("Shoot Garden collection"),
        TURN_TO_FLOWER("3/5 Turn toward rear Flower"),

        // Shared / Far States
        TO_FLOWER_INTAKE("Approach rear Flower"),
        SHOOT_FLOWER("Shoot Flower collection"),
        TO_PARK("Rear Flower to park"),
        SHOT_TIMEOUT("Shot timed out"),
        DONE("");

        final String label;
        State(String label) { this.label = label; }
    }

    private final AutoContext context;
    private final boolean pathTest;
    private final boolean waitForHive;
    private final boolean isClose;
    private final TurretAim.Cell initialCell;
    private final TurretAim.Cell secondaryCell;

    private final StallProtectedPathAction flowerApproach;
    private final StallProtectedPathAction gardenApproach;
    private final Path parkPath;

    private final Action shoot;
    private final Action flowerCollect;
    private final Action gardenCollect;
    private final ElapsedTime collectionTimer = new ElapsedTime();
    private final ElapsedTime waitTimer = new ElapsedTime();
    private Pose shimmyCenter;
    private int lastShimmyStep;
    private final StateMachine machine;
    private boolean previousAdvance;
    private boolean advancePressed;
    private boolean waitingForAdvance;
    private Result result = Result.READY;

    private boolean isHiveTipped = false;

    public TwoTipRoutine(AutoContext context, boolean waitForHive) {
        this.context = context;
        this.waitForHive = waitForHive;
        this.pathTest = context.mode == AutoContext.Mode.PATH_TEST;
        this.isClose = context.config.startSide == StartSide.AUDIENCE;

        if (isClose) {
            initialCell = TurretAim.Cell.CLOSE;
            secondaryCell = TurretAim.Cell.FAR;
        } else {
            initialCell = TurretAim.Cell.FAR;
            secondaryCell = TurretAim.Cell.CLOSE;
        }

        TwoTipPaths paths = new TwoTipPaths(context.config.startPose);

        // Map paths based on start side
        if (isClose) {
            gardenApproach = new StallProtectedPathAction(context.follower, paths.startToGarden);
            flowerApproach = new StallProtectedPathAction(context.follower, paths.rearFlowerApproach);
            parkPath = paths.flowerToPark;
        } else {
            gardenApproach = null; // Garden is skipped on the Far route
            flowerApproach = new StallProtectedPathAction(context.follower, paths.farPreloadToRearFlower);
            parkPath = paths.farRearFlowerToPark;
        }

        shoot = context.mechanismAction(robot -> new ShootAction(robot.outtake, robot.intake, () -> true, 5.0));
        flowerCollect = context.mechanismAction(robot ->
                new CollectAction(robot.intake, 4, FLOWER_WAIT_SECONDS + FLOWER_SHIMMY_SECONDS, 0.15));
        gardenCollect = context.mechanismAction(robot ->
                new CollectAction(robot.intake, 4, GARDEN_WAIT_SECONDS + GARDEN_SHIMMY_SECONDS, 0.15));
        machine = buildMachine(paths);
    }

    private StateMachine buildMachine(TwoTipPaths paths) {
        Follower follower = context.follower;
        Pose turnTarget = FieldPoses.TwoTip.REAR_FLOWER_FACING_WALL;
        return new StateMachineBuilder()
                .state(State.WAIT_FOR_HIVE)
                .transition(() -> !waitForHive || isHiveTipped || waitTimer.seconds() > HIVE_WAIT_TIMEOUT_SECONDS || (pathTest && advancePressed),
                        State.SHOOT_PRELOAD, () -> {
                            context.runMechanism(robot -> robot.outtake.enableShooting(context.config.alliance, initialCell));
                            shoot.start();
                        })

                .state(State.SHOOT_PRELOAD)
                .loop(shoot::update)
                .transition(this::shotTimedOut, State.SHOT_TIMEOUT)
                // BRANCH: Close goes to Garden. Far skips directly to the Flower.
                .transition(() -> shotComplete() && (!pathTest || advancePressed),
                        isClose ? State.TO_GARDEN : State.TO_FLOWER_INTAKE,
                        () -> {
                            context.runMechanism(robot -> {
                                robot.intake.collect();
                                if (!isClose) robot.outtake.setTargetCell(secondaryCell);
                            });
                            if (isClose) gardenApproach.start();
                            else flowerApproach.start();
                })

                // ==========================================
                // Close Route Specific States
                // ==========================================
                .state(State.TO_GARDEN)
                .loop(() -> { if (isClose) gardenApproach.update(); })
                .transition(() -> isClose && canLeaveIntakePath(gardenApproach.result() == Action.Result.SUCCEEDED
                        || gardenApproach.result() == Action.Result.TIMED_OUT, gardenCollect,
                        GARDEN_WAIT_SECONDS, GARDEN_SHIMMY_INCHES), State.CROSS_FIELD, () -> {
                    context.runMechanism(robot -> robot.outtake.setTargetCell(secondaryCell));
                    follower.follow(paths.gardenToRearFlowerLineup);
                })
                .state(State.CROSS_FIELD)
                .transition(this::canAdvanceAfterDrive, State.SHOOT_GARDEN, shoot::start)
                .state(State.SHOOT_GARDEN)
                .loop(shoot::update)
                .transition(this::shotTimedOut, State.SHOT_TIMEOUT)
                .transition(this::shotComplete, State.TURN_TO_FLOWER, () -> follower.hold(turnTarget))
                .state(State.TURN_TO_FLOWER)
                .transition(() -> canAdvanceAfter(atTurnTarget(follower.pose(), turnTarget)), State.TO_FLOWER_INTAKE, () -> {
                    context.runMechanism(robot -> {
                        robot.intake.ramp.setRampDown();
                        robot.intake.collect();
                    });
                    flowerApproach.start();
                })

                // ==========================================
                // Shared States (Far joins back in here)
                // ==========================================
                .state(State.TO_FLOWER_INTAKE)
                .loop(flowerApproach::update)
                // A stall stops the drivetrain, then collection continues just as it does at the endpoint.
                .transition(() -> canLeaveIntakePath(flowerApproach.result() == Action.Result.SUCCEEDED
                                || flowerApproach.result() == Action.Result.TIMED_OUT,
                        flowerCollect, FLOWER_WAIT_SECONDS, FLOWER_SHIMMY_INCHES),
                        State.SHOOT_FLOWER, shoot::start)
                .state(State.SHOOT_FLOWER)
                .loop(shoot::update)
                .transition(this::shotTimedOut, State.SHOT_TIMEOUT)
                .transition(this::shotComplete, State.TO_PARK, () -> follower.follow(parkPath))

                .state(State.TO_PARK)
                .transition(() -> !follower.isBusy(), State.DONE, () -> context.runMechanism(robot -> {
                    robot.intake.ramp.setRampUp();
                    // The OpMode stops this loop, so apply the servo command now.
                    robot.intake.ramp.update();
                }))
                .state(State.SHOT_TIMEOUT)
                .state(State.DONE)
                .build();
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
                // Back, forward to center, left, right to center in the robot's frame.
                double forward = step == 0 ? -shimmyInches : 0;
                double left = step == 2 ? shimmyInches : 0;
                double h = shimmyCenter.heading();
                follower.hold(new Pose(shimmyCenter.x() + forward * Math.cos(h) - left * Math.sin(h),
                        shimmyCenter.y() + forward * Math.sin(h) + left * Math.cos(h), h));
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
        return current.distance(target) <= 1.5 && Math.abs(error) <= Math.toRadians(5);
    }

    @Override public void start() {
        previousAdvance = context.pathTestAdvanceDown();
        advancePressed = false;
        waitingForAdvance = false;
        shimmyCenter = null;
        result = Result.RUNNING;

        waitTimer.reset();
        machine.setState(State.WAIT_FOR_HIVE);
        machine.start();
        machine.update();
    }

    @Override public void update() {
        if (result != Result.RUNNING) return;

        boolean pressed = context.pathTestAdvanceDown();
        advancePressed = pressed && !previousAdvance;
        previousAdvance = pressed;

        if (waitForHive && machine.getState() == State.WAIT_FOR_HIVE) {
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
        if (flowerApproach != null) flowerApproach.stop();
        if (gardenApproach != null) gardenApproach.stop();
        if (!isFinished()) result = Result.CANCELLED;
    }

    @Override public Result result() { return result; }

    @SuppressLint("DefaultLocale")
    @Override public String state() {
        if (result == Result.TIMED_OUT) return ((State) machine.getState()).label;
        if (result != Result.RUNNING) return result.name();
        State current = (State) machine.getState();

        if (current == State.WAIT_FOR_HIVE) {
            if (!waitForHive) return "Skipping hive wait";
            if (pathTest) return "Press A to skip hive wait";
            return String.format("Waiting for hive tip (%.1fs elapsed)", waitTimer.seconds());
        }
        if (shimmyCenter != null) {
            String location = current == State.TO_GARDEN ? "Garden" : "Flower";
            return (lastShimmyStep < 0 ? "Collecting at " : "Shimmy at ") + location;
        }

        if (pathTest && current == State.SHOOT_PRELOAD) {
            return "Press A for " + (isClose ? State.TO_GARDEN.label : State.TO_FLOWER_INTAKE.label);
        }

        if (waitingForAdvance) return "Press A to advance | Finished " + current.label;
        return "Running " + current.label;
    }
}