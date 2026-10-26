package org.firstinspires.ftc.teamcode.opmodes.tests;

import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.utility.EdgeDetector;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Stationary dataset collection; only this OpMode connects to download_snapshots.py. */
@TeleOp(name = "Vision Snapshot Collector", group = "Tests")
public class VisionSnapshotCollector extends OpMode {
    private static final String LAPTOP_IP = "192.168.43.9";
    private static final int PORT = 6000;

    private Limelight3A limelight;
    private EdgeDetector snapshotButton;
    private ExecutorService network;
    private Socket socket;
    private OutputStream out;
    private volatile String connectionStatus = "Disconnected";
    private int snapshotCount;

    @Override
    public void init() {
        limelight = hardwareMap.get(Limelight3A.class, "limelight");
        limelight.pipelineSwitch(0);
        limelight.start();
        snapshotCount = 0;
        snapshotButton = new EdgeDetector(this::takeSnapshot);
        socket = new Socket();
        network = Executors.newSingleThreadExecutor();
        telemetry.addLine("Run download_snapshots.py on the laptop, then press Start.");
        telemetry.addLine("Gamepad 1 A saves a snapshot. This OpMode does not drive the robot.");
    }

    @Override
    public void start() {
        connectionStatus = "Connecting to " + LAPTOP_IP;
        network.execute(() -> {
            try {
                socket.connect(new InetSocketAddress(LAPTOP_IP, PORT), 2000);
                out = socket.getOutputStream();
                connectionStatus = "Connected";
            } catch (IOException e) {
                connectionStatus = "Connection failed: " + e.getMessage();
            }
        });
    }

    @Override
    public void loop() {
        snapshotButton.update(gamepad1.a);
        telemetry.addData("Laptop", connectionStatus);
        telemetry.addData("Snapshots requested", snapshotCount);
        telemetry.addLine("Gamepad 1 A: snapshot");
        telemetry.update();
    }

    private void takeSnapshot() {
        String name = "dataset_snap_" + (++snapshotCount);
        limelight.captureSnapshot(name);
        // A single worker keeps connection and notification operations in order.
        network.execute(() -> {
            if (out == null) return;
            try {
                out.write(("SNAP:" + name + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (IOException e) {
                connectionStatus = "Send failed: " + e.getMessage();
            }
        });
    }

    @Override
    public void stop() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            connectionStatus = "Close failed: " + e.getMessage();
        } finally {
            if (network != null) network.shutdownNow();
            if (limelight != null) limelight.stop();
        }
    }
}
