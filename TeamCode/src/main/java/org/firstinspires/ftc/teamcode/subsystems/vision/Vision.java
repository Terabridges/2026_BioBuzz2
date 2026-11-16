package org.firstinspires.ftc.teamcode.subsystems.vision;

import static com.pedropathing.api.Paths.curve;
import static com.pedropathing.api.Paths.line;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.hardware.dfrobot.HuskyLens;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.util.Utility;
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
    private Utility util;

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
        util = new Utility();
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
        // Define the physical boundaries of the Biobuzz supports
        Pose redSupportStart = new Pose(46.5, 51.0);
        Pose redSupportEnd = new Pose(46.5, 90.0);
        Pose blueSupportStart = new Pose(95.0, 51.0);
        Pose blueSupportEnd = new Pose(95.0, 90.0);

        double redY = robotPose.y();
        double blueY = robotPose.y();

        // Bend direction logic
        double avgY = (robotPose.y() + clusterPose.y()) / 2.0;
        double shiftAmt = (avgY > 72) ? 1.5 : -1.5;

        // Flare control points outward for high speed
        double FLARE_INCHES = 12.0;
        double redControlX = 46.5 - FLARE_INCHES; // FIX 1: Added the minus sign
        double blueControlX = 95.0 + FLARE_INCHES;

        Pose midPose = new Pose((robotPose.x() + clusterPose.x()) / 2, (robotPose.y() + clusterPose.y()) / 2);

        boolean notSafeRed = util.getShortestDistanceVertical(robotPose, midPose, clusterPose, redSupportStart, redSupportEnd) < 9.0;
        boolean notSafeBlue = util.getShortestDistanceVertical(robotPose, midPose, clusterPose, blueSupportStart, blueSupportEnd) < 9.0;

        // FIX 2: Remember what we hit originally so we know which path builder to use at the end!
        boolean buildRedCurve = notSafeRed;
        boolean buildBlueCurve = notSafeBlue;

        while (notSafeRed || notSafeBlue) {

            if (notSafeRed) redY += shiftAmt;
            if (notSafeBlue) blueY += shiftAmt;

            Pose redControl = new Pose(redControlX, redY);
            Pose blueControl = new Pose(blueControlX, blueY);

            // We use our saved booleans here to ensure we consistently use the correct math method
            if (buildRedCurve && buildBlueCurve) {
                notSafeRed = util.getCubicShortestDistanceVertical(robotPose, redControl, blueControl, clusterPose, redSupportStart, redSupportEnd) < 9.0;
                notSafeBlue = util.getCubicShortestDistanceVertical(robotPose, redControl, blueControl, clusterPose, blueSupportStart, blueSupportEnd) < 9.0;
            } else if (buildRedCurve) {
                notSafeRed = util.getShortestDistanceVertical(robotPose, redControl, clusterPose, redSupportStart, redSupportEnd) < 11.0;
            } else {
                notSafeBlue = util.getShortestDistanceVertical(robotPose, blueControl, clusterPose, blueSupportStart, blueSupportEnd) < 11.0;
            }

            // Failsafe
            if (Math.abs(redY - robotPose.y()) > 72 || Math.abs(blueY - robotPose.y()) > 72) {
                break;
            }
        }

        // Build the PedroPathing 3 Path using the trackers we saved
        if (buildRedCurve && buildBlueCurve) {
            // 4-point Cubic Curve
            return curve(robotPose, new Pose(redControlX, redY), new Pose(blueControlX, blueY), clusterPose).linear(robotPose, clusterPose);
        } else if (buildRedCurve) {
            // 3-point Quadratic Curve around Red
            return curve(robotPose, new Pose(redControlX, redY), clusterPose).linear(robotPose, clusterPose);
        } else if (buildBlueCurve) {
            // 3-point Quadratic Curve around Blue
            return curve(robotPose, new Pose(blueControlX, blueY), clusterPose).linear(robotPose, clusterPose);
        } else {
            // Straight Line
            return line(robotPose, clusterPose).linear(robotPose, clusterPose);
        }
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