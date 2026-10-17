package org.firstinspires.ftc.teamcode.subsystems.vision;

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

    public DetectedBall(BallType type, double confidence, int id, double area,
                        double cameraX, double cameraY, double relativeX, double relativeY) {
        this.type = type;
        this.confidence = confidence;
        this.id = id;
        this.area = area;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.relativeX = relativeX;
        this.relativeY = relativeY;
    }

    public BallType getType() {
        return type;
    }
    public double getRelativeX() { return relativeX; }
    public double getRelativeY() { return relativeY; }
    public double getConfidence() { return confidence; }
}
