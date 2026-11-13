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
            { -3.81560,  -23.10245,  1804.45600 },
            {-0.03531,  -45.95972,  3280.48532 },
            { -0.00020,  -0.04550,   1.00000 }
    };

    public DetectedBall(LLResultTypes.DetectorResult result, Pose robotPose) {
        this.type = parseType(result.getClassName());
        this.confidence = result.getConfidence();
        this.id = result.getClassId();
        this.area = result.getTargetArea();
        this.cameraX = result.getTargetXPixels();
        this.cameraY = result.getTargetYPixels();

        double CAMERA_FORWARD_OFFSET_INCHES = 5.5; // Change to actual forward offset
        double CAMERA_LEFT_OFFSET_INCHES = 0.0;    // Change to actual left/right offset
        double PIXELS_PER_INCH = 10.0;

        // 1. Get the target in absolute Canva pixels (Top-Left is 0,0)
        double[] absolutePixels = applyPerspectiveTransform(result.getTargetXPixels(), result.getTargetYPixels());
        double mapPixelX = absolutePixels[0];
        double mapPixelY = absolutePixels[1];

        // 2. Shift origin from Top-Left to Bottom-Center (Camera Lens at 500, 1000)
        double deltaPixelForward = 1000.0 - mapPixelY; // Fixed missing minus sign
        double deltaPixelLateral = mapPixelX - 500.0;

        // 3. Convert to inches and map to PedroPathing axes (X = Forward, Y = Left)
        double targetRelativeToCameraX = deltaPixelForward / PIXELS_PER_INCH;
        double targetRelativeToCameraY = -deltaPixelLateral / PIXELS_PER_INCH;

        // 4. Add the camera's physical offset
        double targetRelativeToRobotCenterX = targetRelativeToCameraX + CAMERA_FORWARD_OFFSET_INCHES;
        double targetRelativeToRobotCenterY = targetRelativeToCameraY + CAMERA_LEFT_OFFSET_INCHES;

        // 5. Rotate by robot heading and add to global robot pose (restored)
        double heading = robotPose.heading();
        this.relativeX = robotPose.x() + (targetRelativeToRobotCenterX * Math.cos(heading) - targetRelativeToRobotCenterY * Math.sin(heading));
        this.relativeY = robotPose.y() + (targetRelativeToRobotCenterX * Math.sin(heading) + targetRelativeToRobotCenterY * Math.cos(heading));
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
