package org.firstinspires.ftc.teamcode.util;

import com.pedropathing.math.Pose; // Note: PP3 often stores Pose here

public class Utility {

    public Utility() {
    }

    public double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /**
     * Checks if a 3-point (Quadratic) curve intersects a vertical line segment.
     */
    public boolean checksVerticalIntersection(Pose start, Pose control, Pose end, Pose segmentStart, Pose segmentEnd) {
        // PedroPathing 3 uses .x() and .y()
        double x1 = start.x();
        double y1 = start.y();
        double cx = control.x();
        double cy = control.y();
        double x2 = end.x();
        double y2 = end.y();

        double x3 = segmentStart.x();
        double y3 = segmentStart.y();
        double y4 = segmentEnd.y();

        // Added explicit multiplication (*) operators
        double A = x1 - 2 * cx + x2;
        double B = 2 * (cx - x1);
        double C = x1 - x3;

        double discriminant = (B * B) - (4 * A * C);

        if (discriminant < 0) return false;

        if (Math.abs(A) < 1e-6) {
            if (Math.abs(B) > 1e-6) {
                double t = -C / B;
                return verifyVerticalBounds(t, y1, cy, y2, y3, y4);
            }
            return false;
        }

        double t1 = (-B + Math.sqrt(discriminant)) / (2 * A);
        double t2 = (-B - Math.sqrt(discriminant)) / (2 * A);

        return verifyVerticalBounds(t1, y1, cy, y2, y3, y4) ||
                verifyVerticalBounds(t2, y1, cy, y2, y3, y4);
    }

    private static boolean verifyVerticalBounds(
            double t, double y1, double cy, double y2, double y3, double y4) {

        if (t < 0 || t > 1) return false;

        // Added explicit multiplication (*) operators
        double intersectY = y1 + t * (2 * cy - 2 * y1) + (t * t) * (y1 - 2 * cy + y2);

        double minY = Math.min(y3, y4);
        double maxY = Math.max(y3, y4);

        return intersectY >= minY && intersectY <= maxY;
    }

    /**
     * Finds the shortest distance between a 3-point (Quadratic) curve and a vertical segment.
     */
    public double getShortestDistanceVertical(Pose start, Pose control, Pose end, Pose segmentStart, Pose segmentEnd) {
        // Restored missing coordinate extraction
        double x1 = start.x();
        double y1 = start.y();
        double cx = control.x();
        double cy = control.y();
        double x2 = end.x();
        double y2 = end.y();

        double x3 = segmentStart.x();
        double y3 = segmentStart.y();
        double y4 = segmentEnd.y();

        double minY = Math.min(y3, y4);
        double maxY = Math.max(y3, y4);
        double minDistance = Double.MAX_VALUE;

        int samples = 100;

        for (int i = 0; i <= samples; i++) {
            double u = (double) i / samples;

            // Added explicit multiplication (*) operators
            double currentX = x1 + u * (2 * cx - 2 * x1) + (u * u) * (x1 - 2 * cx + x2);
            double currentY = y1 + u * (2 * cy - 2 * y1) + (u * u) * (y1 - 2 * cy + y2);

            double closestLineX = x3;
            double closestLineY = Math.max(minY, Math.min(currentY, maxY));

            double dx = currentX - closestLineX;
            double dy = currentY - closestLineY;
            double distance = Math.sqrt((dx * dx) + (dy * dy));

            if (distance < minDistance) {
                minDistance = distance;
            }
        }

        return minDistance;
    }

    /**
     * Finds the shortest distance between a 4-point (Cubic) curve and a vertical segment.
     */
    public double getCubicShortestDistanceVertical(Pose start, Pose control1, Pose control2, Pose end, Pose segmentStart, Pose segmentEnd) {
        double x1 = start.x();
        double y1 = start.y();
        double cx1 = control1.x();
        double cy1 = control1.y();
        double cx2 = control2.x();
        double cy2 = control2.y();
        double x2 = end.x();
        double y2 = end.y();

        double lineX = segmentStart.x();
        double minY = Math.min(segmentStart.y(), segmentEnd.y());
        double maxY = Math.max(segmentStart.y(), segmentEnd.y());

        double minDistance = Double.MAX_VALUE;
        int samples = 100;

        for (int i = 0; i <= samples; i++) {
            double u = (double) i / samples;
            double u1 = 1.0 - u;

            double currentX = (u1 * u1 * u1) * x1
                    + 3 * (u1 * u1) * u * cx1
                    + 3 * u1 * (u * u) * cx2
                    + (u * u * u) * x2;

            double currentY = (u1 * u1 * u1) * y1
                    + 3 * (u1 * u1) * u * cy1
                    + 3 * u1 * (u * u) * cy2
                    + (u * u * u) * y2;

            double closestLineY = Math.max(minY, Math.min(currentY, maxY));

            double dx = currentX - lineX;
            double dy = currentY - closestLineY;
            double distance = Math.sqrt((dx * dx) + (dy * dy));

            if (distance < minDistance) {
                minDistance = distance;
            }
        }

        return minDistance;
    }
}