package org.firstinspires.ftc.teamcode.subsystems.vision;

import com.pedropathing.math.Pose;
import com.qualcomm.hardware.dfrobot.HuskyLens;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.utility.visionUtility.Cluster;
import org.firstinspires.ftc.teamcode.utility.visionUtility.DetectedBall;
import org.psilynx.psikit.core.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Vision implements Subsystem {

    //-------------Hardware------------
    private Limelight3A limelight;
    private HuskyLens huskyLens;

    //-------------Software-------------
    public LLResult latest;
    private int pipeline = 0;

    private boolean huskyLensReady;
    private HuskyLens.Block[] huskyLensTags = new HuskyLens.Block[0];

    private double lastFieldX = 0;
    private double lastFieldY = 0;
    private double lastHeading = 0;
    private long lastTime = 0;
    private double lastPredictedX = 0;
    private double lastPredictedY = 0;

    private final double LOOK_AHEAD_MS = 50; // Predict x ms into the future

    private final double CAMERA_HEIGHT_INCHES = 6.9685;
    private final double TARGET_HEIGHT_INCHES = 2.86; // Estimated height of the pollen object
    private final double CAMERA_PITCH_DEGREES = 20; // Camera angled downwards x degrees
    private final double CAMERA_FORWARD_OFFSET = 4.72441; // Distance from robot center to camera lens
    private final double[][] homographyMatrix = {
            { 3.73030,  53.97050,  -3070.62252 },
            {-3.12343,  54.02959,  1843.95571 },
            { 0.00012,  0.01433,   1.00000 }
    };

    private double smoothedVelocityX = 0;
    private double smoothedVelocityY = 0;
    private final double VELOCITY_SMOOTHING_FACTOR = 0.3; // Lower = smoother but more lag (0.0 to 1.0)
    private long lastCaptureTime = 0;

    //--------Constructor--------
    private Vision(HardwareMap map) {
        limelight = map.get(Limelight3A.class, "limelight");
        huskyLens = map.tryGet(HuskyLens.class, "huskylens");
    }
    private static Vision instance = null;
    public static Vision getInstance(HardwareMap map) {
        if (instance == null) {
            instance = new Vision(map);
        }

        return instance;
    }

    //---------Methods-----------
    private void limelightInit() {
        limelight.pipelineSwitch(pipeline);
        limelight.start();
    }

    private void limelightUpdate() {
        latest = limelight.getLatestResult();
    }

    //---------HuskyLens AprilTags-----------
    private void huskyLensInit() {
        huskyLensTags = new HuskyLens.Block[0];
        huskyLensReady = huskyLens != null && huskyLens.knock();
        if (huskyLensReady) {
            huskyLens.selectAlgorithm(HuskyLens.Algorithm.TAG_RECOGNITION);
        }
    }

    private void huskyLensUpdate() {
        if (!huskyLensReady) return;
        List<HuskyLens.Block> detected = new ArrayList<>();
        for (HuskyLens.Block tag : huskyLens.blocks()) {
            // The SDK can leave null entries for incomplete tag responses.
            if (tag != null) detected.add(tag);
        }
        huskyLensTags = detected.toArray(new HuskyLens.Block[0]);
    }

    public boolean isHuskyLensReady() {
        return huskyLensReady;
    }

    public boolean hasHuskyLensTag() {
        return huskyLensTags.length > 0;
    }

    /** Uses the HuskyLens learned ID, not the official printed AprilTag number. */
    public boolean hasHuskyLensTag(int learnedId) {
        return getHuskyLensTag(learnedId) != null;
    }

    /** Latest raw detections: id, center x/y, width/height, and left/top in pixels. */
    public HuskyLens.Block[] getHuskyLensTags() {
        return huskyLensTags.clone();
    }

    /** Returns the first detection with this learned ID, or null if not detected. */
    public HuskyLens.Block getHuskyLensTag(int learnedId) {
        for (HuskyLens.Block tag : huskyLensTags) {
            if (tag.id == learnedId) return tag;
        }
        return null;
    }

    private void logFeedToLogger() {
        //This will be used for PsiKit later
    }

    public boolean hasTarget() {
        return latest != null && latest.isValid();
    }

    public double getTx() {
        return (hasTarget() ? latest.getTxNC() : 0);
    }

    public double getTy() {
        return (hasTarget() ? latest.getTyNC() : 0);
    }

    public double getArea() {
        return (hasTarget() ? latest.getTa() : 0);
    }

    public Pose getPredictiveInterceptPose(Pose robotPose) {
        LLResult result = limelight.getLatestResult();

        if (result == null || !result.isValid()) {
            resetTrackingHistory();
            return null;
        }
        long currentCaptureTime = (long) result.getTimestamp(); // Or derive from latency if timestamp isn't exposed
        if (currentCaptureTime == lastCaptureTime) {
            return new Pose(lastPredictedX, lastPredictedY, lastHeading);
        }

        List<LLResultTypes.DetectorResult> targets = result.getDetectorResults();
        LLResultTypes.DetectorResult chosenPollen = findBestPollenCluster(targets);

        if (chosenPollen == null) {
            resetTrackingHistory();
            return null;
        }

        // 1. Calculate relative coordinates in inches based on Limelight angles
        double currentXAngle = chosenPollen.getTargetXDegrees();
        double currentYAngle = chosenPollen.getTargetYDegrees();

        double totalPitchRadians = Math.toRadians(CAMERA_PITCH_DEGREES + currentYAngle);

        // Prevent division by zero if target is perfectly on the horizon
        if (Math.abs(totalPitchRadians) < 0.001) totalPitchRadians = 0.001;

        double forwardDistance = (CAMERA_HEIGHT_INCHES - TARGET_HEIGHT_INCHES) / Math.tan(totalPitchRadians);
        double lateralDistance = forwardDistance * Math.tan(Math.toRadians(currentXAngle));

        double robotRelativeForward = forwardDistance + CAMERA_FORWARD_OFFSET;
        double robotRelativeLateral = lateralDistance;

        // Extract robot positions and heading (Pedro Pathing heading is in RADIANS natively)
        double robotX = robotPose.x();
        double robotY = robotPose.y();
        double robotHeadingRad = robotPose.heading();

        // 2. Vector Translation: Convert Robot Space to absolute Field Space coordinates
        // Forward projects along cosine of heading, lateral projects along sine (standard 2D rotation)
        double currentFieldX = robotX + (robotRelativeForward * Math.cos(robotHeadingRad) - robotRelativeLateral * Math.sin(robotHeadingRad));
        double currentFieldY = robotY + (robotRelativeForward * Math.sin(robotHeadingRad) + robotRelativeLateral * Math.cos(robotHeadingRad));

        // 3. Velocity Prediction Math
        long currentTime = System.currentTimeMillis();

        double predictedFieldX = currentFieldX;
        double predictedFieldY = currentFieldY;


        if (lastTime != 0) {
            long deltaTimeMs = currentTime - lastTime;
            if (deltaTimeMs > 0) {
                // Calculate raw instantaneous velocity
                double rawVelocityX = (currentFieldX - lastFieldX) / (double) deltaTimeMs;
                double rawVelocityY = (currentFieldY - lastFieldY) / (double) deltaTimeMs;

                // Apply Exponential Moving Average (EMA) to filter out camera jitter
                smoothedVelocityX = (rawVelocityX * VELOCITY_SMOOTHING_FACTOR) + (smoothedVelocityX * (1 - VELOCITY_SMOOTHING_FACTOR));
                smoothedVelocityY = (rawVelocityY * VELOCITY_SMOOTHING_FACTOR) + (smoothedVelocityY * (1 - VELOCITY_SMOOTHING_FACTOR));

                predictedFieldX = currentFieldX + (smoothedVelocityX * LOOK_AHEAD_MS);
                predictedFieldY = currentFieldY + (smoothedVelocityY * LOOK_AHEAD_MS);
            }
        }

        //Find optimal heading
        double deltaX = predictedFieldX - robotX;
        double deltaY = predictedFieldY - robotY;
        double targetHeadingRad = Math.atan2(deltaY, deltaX);

        // Save current values to track movement velocity on the next frame iteration
        lastFieldX = currentFieldX;
        lastFieldY = currentFieldY;
        lastTime = currentTime;
        lastCaptureTime = currentCaptureTime;
        lastHeading = targetHeadingRad;

        lastPredictedX = predictedFieldX;
        lastPredictedY = predictedFieldY;
        // Return a fresh Pedro Pathing Pose.
        // Note: Target heading can remain 0 or be set to match the robot's heading preference.
        return new Pose(predictedFieldX, predictedFieldY, targetHeadingRad);
    }

    private LLResultTypes.DetectorResult findBestPollenCluster(List<LLResultTypes.DetectorResult> objects) {
        LLResultTypes.DetectorResult bestPollen = null;
        double highestScore = Double.NEGATIVE_INFINITY;

        final double NECTAR_PROXIMITY_THRESHOLD_PIXELS = 15.0;
        final double POLLEN_AREA_MULTIPLIER = 10.0;
        final double NECTAR_PENALTY_WEIGHT = 25.0;

        for (LLResultTypes.DetectorResult obj : objects) {
            if (obj.getClassName().equals("Pollen")) {
                double score = obj.getTargetArea() * POLLEN_AREA_MULTIPLIER;

                double pollenPixelX = obj.getTargetXPixels();
                double pollenPixelY = obj.getTargetYPixels();

                // 2. Scan the rest of the visible list to find nearby nectar threats
                for (LLResultTypes.DetectorResult checkObj : objects) {
                    if (checkObj.getClassName().equals("Red Nectar") || checkObj.getClassName().equals("Blue Nectar")) {

                        double nectarPixelX = checkObj.getTargetXPixels();
                        double nectarPixelY = checkObj.getTargetYPixels();

                        // Calculate straight-line pixel distance (Euclidean Distance Formula)
                        double deltaX = pollenPixelX - nectarPixelX;
                        double deltaY = pollenPixelY - nectarPixelY;
                        double pixelDistance = Math.hypot(deltaX, deltaY);

                        // 3. Apply penalty if nectar is contaminating this specific pollen cluster
                        if (pixelDistance <= NECTAR_PROXIMITY_THRESHOLD_PIXELS) {
                            // The closer or larger the nectar is, the higher the penalty
                            double nectarThreatSize = checkObj.getTargetArea();
                            score -= (nectarThreatSize * NECTAR_PENALTY_WEIGHT);
                        }
                    }
                }

                // 4. Track the single highest scoring (cleanest/largest) target
                if (score > highestScore) {
                    highestScore = score;
                    bestPollen = obj;
                }
            }
        }

        return bestPollen;
    }

    private void resetTrackingHistory() {
        lastFieldX = 0;
        lastFieldY = 0;
        lastTime = 0;
        lastCaptureTime = 0;
        smoothedVelocityX = 0;
        smoothedVelocityY = 0;
        lastHeading = 0;
    }

    public int getCurrentTagId()
    {
        if (!hasTarget() || latest.getFiducialResults().isEmpty()) {
            return -1;
        }
        return latest.getFiducialResults().get(0).getFiducialId();
    }
    public Optional<Pose3D> getLatestBotPose()
    {
        Optional<LLResult> latestOpt = Optional.ofNullable(latest);

        Optional<Pose3D> foo = latestOpt.map(LLResult::getBotpose_MT2);
        if (foo.isEmpty()) {
            return latestOpt.map(LLResult::getBotpose);
        }

        return foo;
    }

    private List<LLResultTypes.DetectorResult> detectorResults() {
        return Optional.ofNullable(latest).map(LLResult::getDetectorResults).orElseGet(Collections::emptyList);
    }

    private DetectedBall toDetectedBall(LLResultTypes.DetectorResult result, Pose robotPose) {
        DetectedBall.BallType type = parseType(result.getClassName());

        double cameraX = result.getTargetXPixels();
        double cameraY = result.getTargetYPixels();

        double[] relativeCoords = applyPerspectiveTransform(cameraX, cameraY);
        double maybeWrongX = relativeCoords[0];
        double maybeWrongY = relativeCoords[1];

        double headingRad = robotPose.heading();
        double relativeX = robotPose.x() + (maybeWrongX * Math.cos(headingRad) - maybeWrongY * Math.sin(headingRad));
        double relativeY = robotPose.y() + (maybeWrongX * Math.sin(headingRad) + maybeWrongY * Math.cos(headingRad));

        return new DetectedBall(
                type,
                result.getConfidence(),
                result.getClassId(),
                result.getTargetArea(),
                cameraX, cameraY,
                relativeX, relativeY
        );
    }

    public List<Cluster> getClusters(Pose robotPose) {
        List<Cluster> clusters = new ArrayList<>();

        for (LLResultTypes.DetectorResult res : detectorResults()) {
            DetectedBall ball = toDetectedBall(res, robotPose);
            boolean added = false;

            for (Cluster cluster : clusters) {
                if (cluster.belongs(ball)) {
                    cluster.add(ball);
                    added = true;
                    break;
                }
            }

            if (!added) {
                Cluster newCluster = new Cluster();
                newCluster.add(ball);
                clusters.add(newCluster);
            }
        }

        return clusters;
    }
    private DetectedBall.BallType parseType(String name) {
        if ("red nectar".equalsIgnoreCase(name)) return DetectedBall.BallType.RED;
        if ("blue nectar".equalsIgnoreCase(name)) return DetectedBall.BallType.BLUE;
        return DetectedBall.BallType.POLLEN;
    }

    private double[] applyPerspectiveTransform(double camX, double camY) {
        double[][] H = homographyMatrix;

        // Matrix-vector multiplication [H] * [camX, camY, 1]^T
        double targetX = H[0][0] * camX + H[0][1] * camY + H[0][2];
        double targetY = H[1][0] * camX + H[1][1] * camY + H[1][2];
        double w       = H[2][0] * camX + H[2][1] * camY + H[2][2];

        // Normalize by homogenous coordinate scale factor w
        if (w != 0.0) {
            targetX /= w;
            targetY /= w;
        }

        return new double[]{ targetX, targetY };
    }

    //---------Interface Methods---------
    @Override
    public void init() {
        huskyLensInit();
        limelightInit();
    }

    @Override
    public void logPsiKitData() {
        Logger.recordOutput("Has Target?", hasTarget());
        Logger.recordOutput("Object(s) Tx:", getTx());
        Logger.recordOutput("Objects(s) Ty: ", getTy());
        Logger.recordOutput("Object(s) Area:", getArea());
    }

    @Override
    public void update() {
        limelightUpdate();
        huskyLensUpdate();
    }

    @Override
    public void stop() {
        huskyLensTags = new HuskyLens.Block[0];
        huskyLensReady = false;
        limelight.stop();
    }

}
