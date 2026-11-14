package org.firstinspires.ftc.teamcode.subsystems.vision;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.hardware.dfrobot.HuskyLens;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.psilynx.psikit.core.Logger;

import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Vision implements Subsystem {

    //-------------Hardware------------
    private Limelight3A limelight;

    //-------------Software-------------
    private LLResult latest;
    private int pipeline = 0;
    //------------Pictures--------------
    private int snapshotCount = 0;
    private static final String LAPTOP_IP = "192.168.43.73";
    private static final int PORT = 6000;

    private Socket socket;
    private OutputStream out;
    private String connectionStatus = "Disconnected"; //Status tracker

    //--------Constructor--------
    private Vision(HardwareMap map) {
        limelight = map.get(Limelight3A.class, "limelight");
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

    private List<LLResultTypes.DetectorResult> detectorResults() {
        return Optional.ofNullable(latest).map(LLResult::getDetectorResults).orElseGet(Collections::emptyList);
    }

    public List<Cluster> getClusters(Pose robotPose) {
        List<DetectedBall> balls = detectorResults()
                .stream()
                .map(res -> new DetectedBall(res, robotPose))
                // THE FIX: Discard asymptote blowouts!
                .filter(ball -> {
                    // (Note: ball.getRelativeX() actually holds the GLOBAL coordinate in your code)
                    double distance = Math.hypot(ball.getRelativeX() - robotPose.x(), ball.getRelativeY() - robotPose.y());
                    return distance < 100.0; // Ignore anything calculating further than 100 inches away
                })
                .collect(Collectors.toList());

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
        return getClusters(robotPose).stream()
                .max(Comparator.comparingDouble(Cluster::score));
    }

    /**
     *
     * @param robotPose Takes in the robot pose directly from follower
     * @param offsetInches Distance away from center of robot to center of cluster to stop
     * @return Unwrapped Robot Pose
     */
    public Pose getBestClusterPose(Pose robotPose, double offsetInches) {
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
        }).orElseGet(() -> new Pose(0,0,0));
    }

    public Pose getBestClusterPose(Pose robotPose) {
        return getBestClusterPose(robotPose, 0.0);
    }

    public Path driveToBestCluster(Pose robotPose, Pose clusterPose) {
//        Pose absolutePose = new Pose(robotPose.x() + clusterPose.x(), robotPose.y() + clusterPose.y(), robotPose.heading() + clusterPose.heading());
//        for (double i = 0; i < 1; i += 0.01) {
//
//        }
        return line(robotPose, clusterPose).linear(robotPose, clusterPose);
    }

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
    //TODO: Solve problem where driving directly to pollen may not be possible and trying to create a new path on top of existing path could be catastrophic

    //---------Interface Methods---------
    @Override
    public void toInit() {
        limelightInit();
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
    }

    @Override
    public void stop() {
        stopLimelightAndServers();
//        limelight.stop();
    }

}