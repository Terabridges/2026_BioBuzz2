package org.firstinspires.ftc.teamcode.subsystems.vision;

import com.pedropathing.math.Pose;
import com.qualcomm.hardware.dfrobot.HuskyLens;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.utility.visionUtility.Cluster;
import org.firstinspires.ftc.teamcode.utility.visionUtility.DetectedBall;
import org.psilynx.psikit.core.Logger;

import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Vision implements Subsystem {

    //-------------Hardware------------
    private Limelight3A limelight;
    private HuskyLens huskyLens;

    //-------------Software-------------
    private LLResult latest;
    private int pipeline = 0;

    private boolean huskyLensReady;
    private HuskyLens.Block[] huskyLensTags = new HuskyLens.Block[0];

    private final double LOOK_AHEAD_MS = 50; // Predict x ms into the future

    private final double CAMERA_HEIGHT_INCHES = 6.9685;
    private final double TARGET_HEIGHT_INCHES = 2.86; // Estimated height of the pollen object
    private final double CAMERA_PITCH_DEGREES = 20; // Camera angled downwards x degrees
    private final double CAMERA_FORWARD_OFFSET = 4.72441; // Distance from robot center to camera lens

    private double smoothedVelocityX = 0;
    private double smoothedVelocityY = 0;
    private final double VELOCITY_SMOOTHING_FACTOR = 0.3; // Lower = smoother but more lag (0.0 to 1.0)
    private long lastCaptureTime = 0;

    //------------Pictures--------------
    private int snapshotCount = 0;
    private static final String LAPTOP_IP = "192.168.43.9";
    private static final int PORT = 6000;

    private Socket socket;
    private OutputStream out;
    private String connectionStatus = "Disconnected"; //Status tracker

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

    //-------HuskyLens AprilTags---------
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

    public int getCurrentTagId()
    {
        if (!hasTarget() || latest.getFiducialResults().isEmpty()) {
            return -1;
        }
        return latest.getFiducialResults().get(0).getFiducialId();
    }

//    public Optional<Pose3D> getLatestBotPose()
//    {
//        Optional<LLResult> latestOpt = Optional.ofNullable(latest);
//
//        Optional<Pose3D> foo = latestOpt.map(LLResult::getBotpose_MT2);
//        if (foo.isEmpty()) {
//            return latestOpt.map(LLResult::getBotpose);
//        }
//
//        return foo;
//    }

    private List<LLResultTypes.DetectorResult> detectorResults() {
        return Optional.ofNullable(latest).map(LLResult::getDetectorResults).orElseGet(Collections::emptyList);
    }

    public List<Cluster> getClusters(Pose robotPose) {
        List<DetectedBall> balls = detectorResults()
                .stream()
                .map(res -> new DetectedBall(res, robotPose))
                .collect(Collectors.toList()
        );

        List<Cluster> clusters = new ArrayList<>();

        for (DetectedBall ball : balls) {
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

    public Optional<Cluster> getBestCluster(Pose robotPose) {
        List<Cluster> clusters = getClusters(robotPose);

        Optional<Cluster> bestCluster = Optional.empty();
        double maxScore  = Double.NEGATIVE_INFINITY;

        for (Cluster cluster: clusters) {
            double currentScore = cluster.score();
            if (currentScore > maxScore) {
                maxScore = currentScore;
                bestCluster = Optional.of(cluster);
            }
        }

        return bestCluster;
    }

    public Optional<Pose> getBestClusterPose(Pose robotPose, double offsetInches) {
        return getBestCluster(robotPose).map(cluster -> {
            double targetX = cluster.getCenterX();
            double targetY = cluster.getCenterY();

            double deltaX = targetX - robotPose.x();
            double deltaY = targetY - robotPose.y();
            double targetHeadingRad = Math.atan2(deltaY, deltaX);

            // Back off by offsetInches along the approach angle
            double approachX = targetX - (offsetInches * Math.cos(targetHeadingRad));
            double approachY = targetY - (offsetInches * Math.sin(targetHeadingRad));

            return new Pose(approachX, approachY, targetHeadingRad);
        });
    }

    public Optional<Pose> getBestClusterPose(Pose robotPose) {
        return getBestClusterPose(robotPose, 0.0);
    }

    //TODO: Solve problem where driving directly to pollen may not be possible and trying to create a new path on top of existing path could be catastrophic

    //-------------Snapshot Methods---------------
    public void startSnapshotServer() {
        connectionStatus = "Attempting to connect to " + LAPTOP_IP + "...";
        new Thread(() -> {
            try {
                socket = new Socket(LAPTOP_IP, PORT);
                out = socket.getOutputStream();
                connectionStatus = "CONNECTED to Laptop!";
            } catch (Exception e) {
                connectionStatus = "FAILED: " + e.getMessage();
            }
        }).start();
    }

    public void takeSnapshot() {
        snapshotCount++;
        String snapName = "dataset_snap_" + snapshotCount;
        limelight.captureSnapshot(snapName);

        new Thread(() -> {
            try {
                if (out != null) {
                    out.write(("SNAP:" + snapName + "\n").getBytes());
                    out.flush();
                }
            } catch (Exception e) {
                connectionStatus = "SEND ERROR: " + e.getMessage();
            }
        }).start();
    }

    public void stopSnapshotServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {
            System.out.println("An error occurred...");
        }
    }

    public void stopLimelightAndServers() {
        limelight.stop();
        stopSnapshotServer();
    }

    // Getters for Telemetry
    public String getConnectionStatus() { return connectionStatus; }
    public int getSnapshotCount() { return snapshotCount; }

    //---------Interface Methods---------
    @Override
    public void init() {
        limelightInit();
        huskyLensInit();
        startSnapshotServer();
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
        stopLimelightAndServers();
    }

}
