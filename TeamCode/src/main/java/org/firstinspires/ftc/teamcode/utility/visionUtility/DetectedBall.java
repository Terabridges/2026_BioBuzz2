package org.firstinspires.ftc.teamcode.utility.visionUtility;

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
            { 3.73030,  53.97050,  -3070.62252 },
            {-3.12343,  54.02959,  1843.95571 },
            { 0.00012,  0.01433,   1.00000 }
    };

    public DetectedBall(LLResultTypes.DetectorResult result, Pose robotPose) {
        this.type = parseType(result.getClassName());
        this.confidence = result.getConfidence();
        this.id = result.getClassId();
        this.area = result.getTargetArea();
        this.cameraX = result.getTargetXPixels();
        this.cameraY = result.getTargetYPixels();

        double[] relOffset = applyPerspectiveTransform(result.getTargetXPixels(), result.getTargetYPixels());

        // 2. Rotate offset by robot heading and add to robot pose
        double heading = robotPose.heading();
        this.relativeX = robotPose.x() + (relOffset[0] * Math.cos(heading) - relOffset[1] * Math.sin(heading));
        this.relativeY = robotPose.y() + (relOffset[0] * Math.sin(heading) + relOffset[1] * Math.cos(heading));
    }

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
