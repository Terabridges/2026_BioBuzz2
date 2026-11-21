package org.firstinspires.ftc.teamcode.autonomous.routines;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.sfdev.assembly.state.StateMachine;
import com.sfdev.assembly.state.StateMachineBuilder;
import org.firstinspires.ftc.teamcode.actions.Action;
import org.firstinspires.ftc.teamcode.actions.CollectAction;
import org.firstinspires.ftc.teamcode.actions.ShootAction;
import org.firstinspires.ftc.teamcode.utility.shooterUtility.TurretAim;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoContext;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoRoutine;
import org.firstinspires.ftc.teamcode.autonomous.paths.FieldPoses;
import org.firstinspires.ftc.teamcode.autonomous.paths.StallProtectedPathAction;
import org.firstinspires.ftc.teamcode.autonomous.paths.TwoTipPaths;

/** Red Audience Two Tip route, with a manual checkpoint before each movement in path test mode. */
public final class TwoTipRoutine implements AutoRoutine {
    private static final double GARDEN_WAIT_SECONDS = 0.75;
    private static final double GARDEN_SHIMMY_SECONDS = 1.5;
    private static final double FLOWER_WAIT_SECONDS = 1.0;
    private static final double FLOWER_SHIMMY_SECONDS = 1.25;
    private static final double SHIMMY_STEP_SECONDS = 0.25;
    private static final double GARDEN_SHIMMY_INCHES = 2.0;
    private static final double FLOWER_SHIMMY_INCHES = 1.0;

    private enum State {
        SHOOT_PRELOAD("Shoot preload"),
        TO_GARDEN("1/5 Start to Garden"),
        CROSS_FIELD("2/5 Garden to rear Flower lineup"),
        SHOOT_GARDEN("Shoot Garden collection"),
        TURN_TO_FLOWER("3/5 Turn toward rear Flower"),
        TO_FLOWER_INTAKE("4/5 Approach rear Flower"),
        SHOOT_FLOWER("Shoot Flower collection"),
        TO_PARK("5/5 Rear Flower to park"),
        SHOT_TIMEOUT("Shot timed out"),
        DONE("");

        final String label;

        State(String label) {
            this.label = label;
        }
    }

    private final AutoContext context;
    private final boolean pathTest;
    private final StallProtectedPathAction flowerApproach;
    private final Action shoot;
    private final Action flowerCollect;
    private final Action gardenCollect;
    private final ElapsedTime collectionTimer = new ElapsedTime();
    private Pose shimmyCenter;
    private int lastShimmyStep;
    private final StateMachine machine;
    private boolean previousAdvance;
    private boolean advancePressed;
    private boolean waitingForAdvance;
    private Result result = Result.READY;

    public TwoTipRoutine(AutoContext context) {
        this.context = context;
        pathTest = context.mode == AutoContext.Mode.PATH_TEST;
        TwoTipPaths paths = new TwoTipPaths(context.config.startPose);
        flowerApproach = new StallProtectedPathAction(context.follower, paths.rearFlowerApproach);
        shoot = context.mechanismAction(robot -> new ShootAction(robot.outtake, robot.intake, () -> true, 5.0));
        flowerCollect = context.mechanismAction(robot ->
                new CollectAction(robot.intake, 4, FLOWER_WAIT_SECONDS + FLOWER_SHIMMY_SECONDS));
        gardenCollect = context.mechanismAction(robot ->
                new CollectAction(robot.intake, 4, GARDEN_WAIT_SECONDS + GARDEN_SHIMMY_SECONDS));
        machine = buildMachine(paths);
    }

    private StateMachine buildMachine(TwoTipPaths paths) {
        Follower follower = context.follower;
        Pose turnTarget = FieldPoses.TwoTip.REAR_FLOWER_FACING_WALL;
        return new StateMachineBuilder()
                .state(State.SHOOT_PRELOAD)
                .loop(shoot::update)
                .transition(this::shotTimedOut, State.SHOT_TIMEOUT)
                .transition(() -> shotComplete() && (!pathTest || advancePressed), State.TO_GARDEN, () -> {
                    context.runMechanism(robot -> robot.intake.collect());
                    follower.follow(paths.startToGarden);
                })
                .state(State.TO_GARDEN)
                .transition(() -> canLeaveIntakePath(!follower.isBusy(), gardenCollect,
                        GARDEN_WAIT_SECONDS, GARDEN_SHIMMY_INCHES), State.CROSS_FIELD, () -> {
                    context.runMechanism(robot -> robot.outtake.setTargetCell(TurretAim.Cell.FAR));
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
                .transition(this::shotComplete, State.TO_PARK, () -> follower.follow(paths.flowerToPark))

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
            // Require a press after the robot has stopped, even if A was pressed this loop.
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
        context.runMechanism(robot -> robot.outtake.enableShooting(context.config.alliance, TurretAim.Cell.CLOSE));
        shoot.start();
        machine.setState(State.SHOOT_PRELOAD);
        machine.start();
        machine.update();
    }

    @Override public void update() {
        if (result != Result.RUNNING) return;

        boolean pressed = context.pathTestAdvanceDown();
        advancePressed = pressed && !previousAdvance;
        previousAdvance = pressed;

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
        flowerApproach.stop();
        if (!isFinished()) result = Result.CANCELLED;
    }

    @Override public Result result() { return result; }

    @Override public String state() {
        if (result == Result.TIMED_OUT) return ((State) machine.getState()).label;
        if (result != Result.RUNNING) return result.name();
        State current = (State) machine.getState();
        if (shimmyCenter != null) {
            String location = current == State.TO_GARDEN ? "Garden" : "Flower";
            return (lastShimmyStep < 0 ? "Collecting at " : "Shimmy at ") + location;
        }
        if (pathTest && current == State.SHOOT_PRELOAD) return "Press A for " + State.TO_GARDEN.label;
        if (waitingForAdvance) return "Press A to advance | Finished " + current.label;
        return "Running " + current.label;
    }
}
