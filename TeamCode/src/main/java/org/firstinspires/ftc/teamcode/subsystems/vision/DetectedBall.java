package org.firstinspires.ftc.teamcode.subsystems.vision;

import com.pedropathing.math.Pose;
import com.qualcomm.hardware.limelightvision.LLResultTypes;

public class DetectedBall {
    public enum BallType { POLLEN, RED, BLUE}

    private final BallType type;
    private final double confidence;
    private final double id;
    private final double area;
    private final double cameraX;
    private final double cameraY;
    private final double relativeX;
    private final double relativeY;

    private static final double[][] HOMOGRAPHY_MATRIX = {
            {  2.77112,  12.08609,  -808.73681 },
            { -0.76524,  23.86132, -2028.13303 },
            { -0.00247,   0.03815,     1.00000 }
    };

    public DetectedBall(LLResultTypes.DetectorResult result, Pose robotPose) {
        this.type = parseType(result.getClassName());
        this.confidence = result.getConfidence();
        this.id = result.getClassId();
        this.area = result.getTargetArea();
        this.cameraX = result.getTargetXPixels();
        this.cameraY = result.getTargetYPixels();

        double ROBOT_CENTER_TO_TILE_EDGE_INCHES = 9.5 + 6.5; // Change to actual forward offset
        double CAMERA_LEFT_OFFSET_INCHES = 0.0;    // Change to actual left/right offset

        // 1. Get the target in absolute Canva pixels (Top-Left is 0,0)
        double[] mapPixels = applyPerspectiveTransform(result.getTargetXPixels(), result.getTargetYPixels());
        double mapPixelX = mapPixels[0];
        double mapPixelY = mapPixels[1];

        // 2. Convert to inches relative to the tile's bottom-center origin (320.0, 327.0)
        // 24.0 inches / 240.0 pixels = 0.1 inches per pixel
        double INCHES_PER_MAP_PIXEL = 24.0 / 240.0;
        double forwardFromTileEdge = (335.0 - mapPixelY) * INCHES_PER_MAP_PIXEL;
        double leftFromTileCenter  = (320.0 - mapPixelX) * INCHES_PER_MAP_PIXEL;

        // 3. Shift origin from tile front edge to the center of the robot
        double targetRelativeToRobotCenterX = forwardFromTileEdge + ROBOT_CENTER_TO_TILE_EDGE_INCHES;
        double targetRelativeToRobotCenterY = leftFromTileCenter + CAMERA_LEFT_OFFSET_INCHES;

        // 4. Rotate by robot heading and add to global robot pose
        double heading = robotPose.heading();
        this.relativeX = robotPose.x() + (targetRelativeToRobotCenterX * Math.cos(heading) - targetRelativeToRobotCenterY * Math.sin(heading));
        this.relativeY = robotPose.y() + (targetRelativeToRobotCenterX * Math.sin(heading) + targetRelativeToRobotCenterY * Math.cos(heading));}

    private double[] applyPerspectiveTransform(double pixelX, double pixelY) {
        double targetX = HOMOGRAPHY_MATRIX[0][0] * pixelX + HOMOGRAPHY_MATRIX[0][1] * pixelY + HOMOGRAPHY_MATRIX[0][2];
        double targetY = HOMOGRAPHY_MATRIX[1][0] * pixelX + HOMOGRAPHY_MATRIX[1][1] * pixelY + HOMOGRAPHY_MATRIX[1][2];
        double w       = HOMOGRAPHY_MATRIX[2][0] * pixelX + HOMOGRAPHY_MATRIX[2][1] * pixelY + HOMOGRAPHY_MATRIX[2][2];

        if (w != 0.0) {
            targetX /= w;
            targetY /= w;
        }

        return new double[]{ targetX, targetY };
    }

    private BallType parseType(String name) {
        if ("red nectar".equalsIgnoreCase(name)) return BallType.RED;
        if ("blue nectar".equalsIgnoreCase(name)) return BallType.BLUE;
        return BallType.POLLEN;
    }

    public BallType getType() { return type; }
    public double getRelativeX() { return relativeX; }
    public double getRelativeY() { return relativeY; }
    public double getConfidence() { return confidence; }
}
