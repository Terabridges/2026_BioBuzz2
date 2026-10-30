package org.firstinspires.ftc.teamcode.utility.visionUtility;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    private final List<DetectedBall> balls = new ArrayList<>();
    private double relativeCenterX;
    private double relativeCenterY;
    private static final double POLLEN_REWARD = 10.0;   // Base reward per pollen ball
    private static final double NECTAR_PENALTY = 25.0;  // Base penalty per nectar ball
    private static final double PURITY_BONUS = 1.5;

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
        if (balls.isEmpty()) {
            return 0.0;
        }

        double netScore = 0.0;
        int pollenCount = 0;
        int nectarCount = 0;

        for (DetectedBall ball: balls) {
            double weight = ball.getConfidence();

            if (ball.getType() == DetectedBall.BallType.POLLEN) {
                netScore += (POLLEN_REWARD * weight);
                pollenCount++;
            } else { // RED or BLUE Nectar
                netScore -= (NECTAR_PENALTY * weight);
                nectarCount++;
            }

        }

        if (pollenCount > 0 && nectarCount == 0) {
            netScore *= PURITY_BONUS;
        }

        return netScore;
    }

    private void recalculateCenter() {
        this.relativeCenterX = balls.stream().mapToDouble(DetectedBall::getRelativeX).average().orElse(0.0);
        this.relativeCenterY = balls.stream().mapToDouble(DetectedBall::getRelativeY).average().orElse(0.0);
    }

    public List<DetectedBall> getBalls() { return balls; }
    public double getCenterX() { return relativeCenterX; }
    public double getCenterY() { return relativeCenterY; }
}
