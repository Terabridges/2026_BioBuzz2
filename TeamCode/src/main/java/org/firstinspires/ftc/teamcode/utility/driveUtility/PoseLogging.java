package org.firstinspires.ftc.teamcode.utility.driveUtility;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.subsystems.Robot;
import org.psilynx.psikit.core.Logger;
import org.psilynx.psikit.core.wpi.math.Pose2d;
import org.psilynx.psikit.core.wpi.math.Rotation2d;

public final class PoseLogging {
    private static final double INCHES_TO_METERS = 0.0254;
    private static final double FIELD_SIZE_IN = 144.0;
    private static final double FIELD_HALF_IN = FIELD_SIZE_IN * 0.5;
    public static void logMainPoseDetails(Robot robot) {
        if (robot == null || robot.outtake == null || robot.outtake.turret == null || robot.vision == null) {
            return;
        }

        Pose followerPose = (robot.drive.getPedroDrive() != null) ? robot.drive.getPedroDrive().robotPose() : null;
        Pose2d pinpointFtcPose = null;
        if (followerPose != null) {
            Logger.recordOutput("Pinpoint/X", followerPose.x());
            Logger.recordOutput("Pinpoint/Y", followerPose.y());
            Logger.recordOutput("Pinpoint/HeadingDeg", Math.toDegrees(followerPose.heading()));
            pinpointFtcPose = toPose2dFromPedroInchesAsFtcCenterRotated(followerPose);
            Logger.recordOutput("Localization/Pinpoint/Pose2d", pinpointFtcPose);
        } else {
            Logger.recordOutput("Pinpoint/X", Double.NaN);
            Logger.recordOutput("Pinpoint/Y", Double.NaN);
            Logger.recordOutput("Pinpoint/HeadingDeg", Double.NaN);
            Logger.recordOutput("Pinpoint/TotalHeadingDeg", Double.NaN);
        }

        if (robot.drive.getPedroDrive() != null &&robot.drive.getPedroDrive().getFollower().velocity() != null) {
            Logger.recordOutput("Pinpoint/VelX", robot.drive.getPedroDrive().getFollower().velocity().vx);
            Logger.recordOutput("Pinpoint/VelY", robot.drive.getPedroDrive().getFollower().velocity().vy);
        } else {
            Logger.recordOutput("Pinpoint/VelX", Double.NaN);
            Logger.recordOutput("Pinpoint/VelY", Double.NaN);
        }

        Logger.recordOutput("Limelight/HasTarget", robot.vision.hasTarget() ? 1.0 : 0.0);
        Logger.recordOutput("Limelight/TagId", robot.vision.getCurrentTagId());
        Logger.recordOutput("Limelight/Tx", robot.vision.getTx());

        Logger.recordOutput("Localization/Primary/Valid", pinpointFtcPose != null ? 1.0 : 0.0);
        if (pinpointFtcPose != null) {
            Logger.recordOutput("Localization/Primary/Pose2d", pinpointFtcPose);
            Logger.recordOutput("Localization/Primary/Source", "PinpointFTC");
        }

    }

    public static Pose2d toPose2dFromPedroInchesAsFtcCenterRotated(Pose pedroPose) {
        if (pedroPose == null) return null;
        double pedroXIn = pedroPose.x();
        double pedroYIn = pedroPose.y();
        double pedroHeadingRad = pedroPose.heading();

        double ftcXIn = FIELD_HALF_IN - pedroYIn;
        double ftcYIn = pedroXIn - FIELD_HALF_IN;
        double ftcHeadingRad = wrapRad(pedroHeadingRad + (Math.PI * 0.5));

        return new Pose2d(
                ftcXIn * INCHES_TO_METERS,
                ftcYIn * INCHES_TO_METERS,
                Rotation2d.fromRadians(ftcHeadingRad)
        );
    }

    public static double[] toPose2dFromPedroInchesAsFtcCenterRotatedWithoutHeading(Pose pedroPose) {
        if (pedroPose == null) return null;
        double pedroXIn = pedroPose.x();
        double pedroYIn = pedroPose.y();

        double ftcXIn = FIELD_HALF_IN - pedroYIn;
        double ftcYIn = pedroXIn - FIELD_HALF_IN;

        return new double[]{
                ftcXIn * INCHES_TO_METERS,
                ftcYIn * INCHES_TO_METERS,
        };
    }

    private static double wrapRad(double radians) {
        return Math.atan2(Math.sin(radians), Math.cos(radians));
    }
}
