package org.firstinspires.ftc.teamcode.subsystems.vision;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    private final List<DetectedBall> balls = new ArrayList<>();
    private double relativeCenterX;
    private double relativeCenterY;

    public boolean belongs(DetectedBall ball) {
        if (balls.isEmpty()) return true;

        double distance = Math.hypot(ball.getRelativeX() - relativeCenterX,
                ball.getRelativeY() - relativeCenterY);
        return distance < 0.5; // Radius threshold in field units (e.g. inches or meters)
    }

    public void add(DetectedBall ball) {
        balls.add(ball);
        recalculateCenter();
    }

    public double score() {
        return balls.stream().mapToDouble(DetectedBall::getConfidence).sum();
    }

    private void recalculateCenter() {
        this.relativeCenterX = balls.stream().mapToDouble(DetectedBall::getRelativeX).average().orElse(0.0);
        this.relativeCenterY = balls.stream().mapToDouble(DetectedBall::getRelativeY).average().orElse(0.0);
    }

    public List<DetectedBall> getBalls() { return balls; }
    public double getCenterX() { return relativeCenterX; }
    public double getCenterY() { return relativeCenterY; }
}
