package org.firstinspires.ftc.teamcode.autonomous.routines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.firstinspires.ftc.teamcode.actions.Action;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoCatalog;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoConfig;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoContext;
import org.firstinspires.ftc.teamcode.autonomous.framework.AutoRoutine;
import org.firstinspires.ftc.teamcode.autonomous.framework.StartSide;
import org.firstinspires.ftc.teamcode.autonomous.paths.FieldPoses;
import org.firstinspires.ftc.teamcode.autonomous.paths.StallProtectedPathAction;
import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.mockito.Mockito.*;
import org.junit.Before;
import org.junit.Test;

public class ThreeTipRoutineTest {
    private final TestFollower follower = new TestFollower();
    private final AtomicBoolean aDown = new AtomicBoolean();
    private AutoCatalog.Entry closeEntry;
    private AutoCatalog.Entry farEntry;

    @Before
    public void setup() {
        closeEntry = AutoCatalog.ENTRIES.stream()
                .filter(candidate -> candidate.config.name.equals("3 Tip Close"))
                .findFirst().orElseThrow(() -> new AssertionError("3 Tip Close routine not found in catalog"));
        farEntry = AutoCatalog.ENTRIES.stream()
                .filter(candidate -> candidate.config.name.equals("3 Tip Far Wait"))
                .findFirst().orElseThrow(() -> new AssertionError("3 Tip Far Wait routine not found in catalog"));

        follower.paths.clear();
        follower.stopCommands = 0;
        follower.turnCommands = 0;
    }

    @Test
    public void testHardwareIsRequired() {
        assertTrue("3 Tip Close should require robot hardware", closeEntry.requiresRobot);
        assertTrue("3 Tip Far Wait should require robot hardware", farEntry.requiresRobot);
    }

    @Test
    public void testAudienceSidePaths_PrintsPathSequence() {
        System.out.println("=== RUNNING AUDIENCE (CLOSE) PATH TEST ===");
        AutoRoutine routine = closeEntry.create(
                new AutoContext(closeEntry.config, follower, null, AutoContext.Mode.PATH_TEST, aDown::get));

        routine.start();
        for (int step = 0; step < 30 && !routine.isFinished(); step++) {
            follower.busy = false;
            updateWithButton(routine, false);
            updateWithButton(routine, true);
        }

        String[] labels = {
                "1. closeStartToGarden",
                "2. closeGardenToSideFlower",
                "3. closeSideFlowerToShoot",
                "4. closeShootToPark"
        };
        for (int i = 0; i < follower.paths.size(); i++) {
            System.out.println(i < labels.length ? labels[i] : "Path " + (i + 1));
            printPath(follower.paths.get(i));
        }

        assertEquals("Audience side should execute all 4 paths", 4, follower.paths.size());
        assertEquals(1, follower.turnCommands);
        assertEquals(AutoRoutine.Result.SUCCEEDED, routine.result());
        System.out.println("==========================================\n");
    }

    @Test
    public void testRearSidePaths_PrintsPathSequence() {
        System.out.println("=== RUNNING REAR (FAR) PATH TEST ===");
        AutoRoutine routine = farEntry.create(
                new AutoContext(farEntry.config, follower, null, AutoContext.Mode.PATH_TEST, aDown::get));

        routine.start();
        for (int step = 0; step < 35 && !routine.isFinished(); step++) {
            follower.busy = false;
            updateWithButton(routine, false);
            updateWithButton(routine, true);
        }

        String[] labels = {
                "1. farStartToRearFlower",
                "2. farRearFlowerToGarden",
                "3. farGardenToSideFlower",
                "4. farSideFlowerToShoot",
                "5. farShootToPark"
        };
        for (int i = 0; i < follower.paths.size(); i++) {
            System.out.println(i < labels.length ? labels[i] : "Path " + (i + 1));
            printPath(follower.paths.get(i));
        }

        assertEquals("Rear side should execute all 5 paths", 5, follower.paths.size());
        assertEquals(1, follower.turnCommands);
        assertEquals(AutoRoutine.Result.SUCCEEDED, routine.result());
        System.out.println("====================================\n");
    }

    @Test
    public void testWaitsForHiveTipped_BlocksProgressionUntilTrue() {
        System.out.println("=== RUNNING WAIT FOR HIVE TEST ===");

        // USE THE CATALOG CONFIG instead of a new one, so requiresRobot = true is preserved!
        AutoConfig config = farEntry.config;

        // 1. Mock the Robot and the subsystems accessed by the routine
        org.firstinspires.ftc.teamcode.subsystems.Robot mockRobot = mock(org.firstinspires.ftc.teamcode.subsystems.Robot.class);

        // Mock the public fields/subsystems so they aren't null when context.runMechanism calls them
        mockRobot.vision = mock(org.firstinspires.ftc.teamcode.subsystems.vision.Vision.class);
        mockRobot.intake = mock(org.firstinspires.ftc.teamcode.subsystems.intake.Intake.class);
        mockRobot.outtake = mock(org.firstinspires.ftc.teamcode.subsystems.outtake.Outtake.class);

        // 2. Simulate HuskyLens NOT seeing the tag initially
        when(mockRobot.vision.hasHuskyLensTag()).thenReturn(false);

        // Pass the mockRobot and the proper config
        AutoRoutine routine = new ThreeTipRoutine(
                new AutoContext(config, follower, mockRobot, AutoContext.Mode.COMPETITION, aDown::get), true);

        routine.start();
        routine.update(); // Update 1: Reads vision (false), stays in wait state

        assertTrue("Routine should block at WAIT_FOR_HIVE_PRELOAD",
                routine.state().contains("Waiting for hive tip"));
        System.out.println("State before tipped: " + routine.state());

        // 3. Simulate HuskyLens detecting the tag
        when(mockRobot.vision.hasHuskyLensTag()).thenReturn(true);

        routine.update(); // Update 2: Reads vision (true), transitions to SHOOT_PRELOAD

        assertFalse("Routine should advance past the wait state once tipped",
                routine.state().contains("Waiting for hive tip"));
        System.out.println("State after tipped: " + routine.state());

        routine.stop();
        System.out.println("==================================\n");
    }

    @Test
    public void testHiveWaitTimeoutFallback_AdvancesAfterFiveSeconds() throws Exception {
        System.out.println("=== RUNNING HIVE WAIT TIMEOUT FALLBACK TEST ===");
        Robot mockRobot = createMockRobot();
        when(mockRobot.vision.hasHuskyLensTag()).thenReturn(false);

        AutoRoutine routine = new ThreeTipRoutine(
                new AutoContext(farEntry.config, follower, mockRobot, AutoContext.Mode.COMPETITION, aDown::get), true);

        routine.start();
        routine.update();
        assertTrue("Should initially block waiting for hive",
                routine.state().contains("Waiting for hive tip"));

        // Fast-forward waitTimer past HIVE_WAIT_TIMEOUT_SECONDS (5.0s)
        setElapsedSeconds(routine, "waitTimer", 5.1);
        routine.update();

        assertFalse("Should fall back and advance past hive wait after 5s timeout",
                routine.state().contains("Waiting for hive tip"));
        System.out.println("State after 5s hive timeout: " + routine.state());
        routine.stop();
        System.out.println("===============================================\n");
    }

    @Test
    public void testShotTimeout_AbortsRoutineWithTimedOutResult() throws Exception {
        System.out.println("=== RUNNING SHOT TIMEOUT ABORT TEST ===");
        Robot mockRobot = createMockRobot();
        when(mockRobot.vision.hasHuskyLensTag()).thenReturn(true);

        AutoRoutine routine = new ThreeTipRoutine(
                new AutoContext(closeEntry.config, follower, mockRobot, AutoContext.Mode.COMPETITION, aDown::get), false);

        routine.start();
        routine.update(); // Transitions from WAIT_FOR_HIVE_PRELOAD -> SHOOT_PRELOAD

        // Swap the private shoot Action with a mock reporting TIMED_OUT
        Action timedOutShoot = mock(Action.class);
        when(timedOutShoot.result()).thenReturn(Action.Result.TIMED_OUT);
        setField(routine, "shoot", timedOutShoot);

        routine.update(); // Evaluates shotTimedOut() -> SHOT_TIMEOUT

        assertTrue("Routine should finish when shot times out", routine.isFinished());
        assertEquals("Result should be TIMED_OUT", AutoRoutine.Result.TIMED_OUT, routine.result());
        System.out.println("Final state on shot timeout: " + routine.state());
        System.out.println("=======================================\n");
    }

    @Test
    public void testPathStallTimeout_RecoversIntoCollectionAndHiveWait() throws Exception {
        System.out.println("=== RUNNING PATH STALL RECOVERY TEST ===");
        Robot mockRobot = createMockRobot();
        when(mockRobot.vision.hasHuskyLensTag()).thenReturn(true);

        AutoRoutine routine = new ThreeTipRoutine(
                new AutoContext(closeEntry.config, follower, mockRobot, AutoContext.Mode.COMPETITION, aDown::get), false);

        // Mock shoot as immediately succeeded so we quickly reach TO_GARDEN
        Action instantShoot = mock(Action.class);
        when(instantShoot.result()).thenReturn(Action.Result.SUCCEEDED);
        setField(routine, "shoot", instantShoot);

        routine.start();
        routine.update(); // WAIT_FOR_HIVE_PRELOAD -> SHOOT_PRELOAD
        routine.update(); // SHOOT_PRELOAD -> TO_GARDEN (Alliance wait moved to AFTER garden approach)

        // Simulate StallProtectedPathAction timing out (e.g., pinned against wall/robot)
        StallProtectedPathAction stalledGarden = mock(StallProtectedPathAction.class);
        when(stalledGarden.result()).thenReturn(Action.Result.TIMED_OUT);
        setField(routine, "gardenApproach", stalledGarden);

        // Mock gardenCollect as finished so canLeaveIntakePath proceeds immediately
        Action instantCollect = mock(Action.class);
        when(instantCollect.isFinished()).thenReturn(true);
        setField(routine, "gardenCollect", instantCollect);

        routine.update(); // Should recover from path TIMED_OUT and advance to WAIT_FOR_ALLIANCE_AND_HIVE_GARDEN

        assertFalse("Routine should not abort on a stalled path", routine.isFinished());
        assertTrue("Routine should recover into garden alliance/hive wait",
                routine.state().contains("Garden: Alliance"));

        System.out.println("State after path stall recovery: " + routine.state());
        routine.stop();
        System.out.println("=========================================\n");
    }
    @Test
    public void testCloseSide_AllScenariosMatrix() throws Exception {
        System.out.println("=== RUNNING CLOSE 3-TIP SCENARIO MATRIX (8 Permutations) ===");
        runStateMatrix(closeEntry.config);
        System.out.println("==============================================================\n");
    }

    @Test
    public void testFarSide_AllScenariosMatrix() throws Exception {
        System.out.println("=== RUNNING FAR 3-TIP SCENARIO MATRIX (8 Permutations) ===");
        runStateMatrix(farEntry.config);
        System.out.println("============================================================\n");
    }

    // --- Matrix Runner & Simulator ---

    private void runStateMatrix(AutoConfig config) throws Exception {
        boolean[] bools = {true, false};

        for (boolean visionSuccess : bools) {
            for (boolean pathSuccess : bools) {
                for (boolean shotSuccess : bools) {
                    String scenarioLabel = String.format("[Vision: %-5b | Paths: %-5b | Shots: %-5b]",
                            visionSuccess, pathSuccess, shotSuccess);

                    simulateScenario(config, scenarioLabel, visionSuccess, pathSuccess, shotSuccess);
                }
            }
        }
    }

    private void simulateScenario(AutoConfig config, String label, boolean vis, boolean path, boolean shot) throws Exception {
        // Use deep stubs so nested calls like mockRobot.intake.ramp.setRampDown() don't return null and crash
        Robot mockRobot = createMockRobot();
        when(mockRobot.vision.hasHuskyLensTag()).thenReturn(vis);

        AutoRoutine routine = new ThreeTipRoutine(
                new AutoContext(config, follower, mockRobot, AutoContext.Mode.COMPETITION, aDown::get), true);

        routine.start();

        // 1. Swap internal Actions with mocks reflecting the scenario's conditions
        Action mockShoot = mock(Action.class);
        when(mockShoot.result()).thenReturn(shot ? Action.Result.SUCCEEDED : Action.Result.TIMED_OUT);
        when(mockShoot.isFinished()).thenReturn(true);
        setField(routine, "shoot", mockShoot);

        StallProtectedPathAction mockPath = mock(StallProtectedPathAction.class);
        when(mockPath.result()).thenReturn(path ? Action.Result.SUCCEEDED : Action.Result.TIMED_OUT);
        when(mockPath.isFinished()).thenReturn(true);

        if (config.startSide == StartSide.REAR) {
            setField(routine, "farRearFlowerApproach", mockPath);
        }
        setField(routine, "gardenApproach", mockPath);
        setField(routine, "sideFlowerApproach", mockPath);

        Action mockCollect = mock(Action.class);
        when(mockCollect.isFinished()).thenReturn(true);
        setField(routine, "gardenCollect", mockCollect);
        setField(routine, "flowerCollect", mockCollect);

        // 2. Extract StateMachine to monitor exact Enum transitions
        java.lang.reflect.Field machineField = routine.getClass().getDeclaredField("machine");
        machineField.setAccessible(true);
        com.sfdev.assembly.state.StateMachine machine = (com.sfdev.assembly.state.StateMachine) machineField.get(routine);

        // 3. Pump the state machine until it terminates
        List<String> flow = new ArrayList<>();
        String lastState = "";
        int steps = 0;

        while (!routine.isFinished() && steps < 100) {
            String currentState = machine.getState().toString();
            if (!currentState.equals(lastState)) {
                flow.add(currentState);
                lastState = currentState;
            }

            follower.busy = false; // Instantly complete standard follower.follow() paths

            // Resolve time-gated transitions dynamically
            if (!vis && currentState.startsWith("WAIT_FOR_HIVE")) {
                setElapsedSeconds(routine, "waitTimer", 5.1); // Fast-forward hive timeout
            }
            if (currentState.equals("WAIT_FOR_ALLIANCE_AND_HIVE_GARDEN")) {
                setElapsedSeconds(routine, "allianceTimer", 4.1); // Fast-forward partner timeout
                if (!vis) {
                    setElapsedSeconds(routine, "waitTimer", 5.1); // Fast-forward hive timeout at the same time
                }
            }
            if (currentState.equals("TURN_TO_SIDE_FLOWER")) {
                // Fulfill atTurnTarget requirement
                Pose target = FieldPoses.ThreeTip.AUDIENCE_FLOWER_FACING_WALL;
                follower.hold(new Pose(0, 0, target.heading()));
            }

            routine.update();
            steps++;
        }

        // 4. Assert and Print
        AutoRoutine.Result expectedResult = shot ? AutoRoutine.Result.SUCCEEDED : AutoRoutine.Result.TIMED_OUT;
        assertEquals("Routine should complete correctly based on shot success", expectedResult, routine.result());

        System.out.println("Scenario " + label + " -> " + routine.result());
        System.out.println("  Flow: " + String.join(" -> ", flow));
        System.out.println();
    }

    // --- Reflection & Mock Helpers ---

    private Robot createMockRobot() {
        Robot mockRobot = mock(Robot.class);

        // I noticed your error output uses a lowercase '.vision.' package here,
        // so adjust the imports below to match your exact file structure!
        mockRobot.vision = mock(org.firstinspires.ftc.teamcode.subsystems.vision.Vision.class);
        mockRobot.intake = mock(org.firstinspires.ftc.teamcode.subsystems.intake.Intake.class);

        // Prevent NPEs on nested calls like robot.intake.ramp.setRampDown()
        mockRobot.intake.ramp = mock(org.firstinspires.ftc.teamcode.subsystems.intake.Ramp.class);

        mockRobot.outtake = mock(org.firstinspires.ftc.teamcode.subsystems.outtake.Outtake.class);
        return mockRobot;
    }

    private static void setElapsedSeconds(Object target, String timerFieldName, double seconds) throws Exception {
        java.lang.reflect.Field timerField = target.getClass().getDeclaredField(timerFieldName);
        timerField.setAccessible(true);
        com.qualcomm.robotcore.util.ElapsedTime timer =
                (com.qualcomm.robotcore.util.ElapsedTime) timerField.get(target);

        java.lang.reflect.Field startField = null;
        for (java.lang.reflect.Field f : com.qualcomm.robotcore.util.ElapsedTime.class.getDeclaredFields()) {
            if (f.getType() == long.class && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                startField = f;
                break;
            }
        }
        if (startField == null) {
            throw new NoSuchFieldException("No non-static long timestamp field found on ElapsedTime");
        }
        startField.setAccessible(true);
        startField.setLong(timer, System.nanoTime() - (long) (seconds * 1_000_000_000L));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void printPath(Path path) {
        Pose start = path.get(0);
        Pose end = path.endPose();
        System.out.printf("  -> Start Pose: X=%.2f, Y=%.2f, Heading=%.2f deg%n",
                start.x(), start.y(), Math.toDegrees(start.heading()));
        System.out.printf("  -> End Pose:   X=%.2f, Y=%.2f, Heading=%.2f deg%n",
                end.x(), end.y(), Math.toDegrees(end.heading()));
    }

    private void updateWithButton(AutoRoutine routine, boolean pressed) {
        aDown.set(pressed);
        routine.update();
    }

    private static final class TestFollower extends Follower {
        private final List<Path> paths = new ArrayList<>();
        private int turnCommands;
        private int stopCommands;
        private boolean busy;
        private Pose pose = Pose.zero();

        TestFollower() {
            super(null, (Drivetrain) Proxy.newProxyInstance(Drivetrain.class.getClassLoader(),
                    new Class<?>[] {Drivetrain.class}, (proxy, method, args) -> null), null);
        }

        @Override public void follow(Path path) {
            paths.add(path);
            busy = true;
            pose = path.endPose();
        }

        @Override public void hold(Pose p) {
            turnCommands++;
            this.pose = p;
        }

        @Override public Pose pose() { return pose; }
        @Override public boolean isBusy() { return busy; }
        @Override public double remainingDistance() { return 10; }

        @Override public void stop() {
            stopCommands++;
            busy = false;
        }
    }
}