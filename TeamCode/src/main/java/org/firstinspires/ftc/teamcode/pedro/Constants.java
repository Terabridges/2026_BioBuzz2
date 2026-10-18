package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.controllers.Controller;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Matrix;
import com.pedropathing.math.Vector2D;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.revhub.localizers.PinpointConfig;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public class Constants {
    public static MecanumConfig drivetrainConfig = new MecanumConfig(c -> {
        c.frontLeftName.set("front_left");
        c.frontRightName.set("front_right");
        c.backLeftName.set("back_left");
        c.backRightName.set("back_right");
        c.frontLeftDirection.set(DcMotorSimple.Direction.FORWARD);
        c.frontRightDirection.set(DcMotorSimple.Direction.REVERSE);
        c.backLeftDirection.set(DcMotorSimple.Direction.FORWARD);
        c.backRightDirection.set(DcMotorSimple.Direction.REVERSE);
        c.manualBrakeMode.set(true);
    });

    public static PinpointConfig localizerConfig = new PinpointConfig(c -> {
        c.name.set("pinpoint");
        c.podType.set(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
        c.xPodOffset.set(-6.337306105245756);
        c.yPodOffset.set(-2.5977968230960875);
        c.xPodDirection.set(GoBildaPinpointDriver.EncoderDirection.REVERSED);
        c.yPodDirection.set(GoBildaPinpointDriver.EncoderDirection.REVERSED);
        c.globalDistanceUnit.set(DistanceUnit.INCH);
        c.offsetUnits.set(DistanceUnit.INCH);
    });
    public static ForesightConfig foresightConfig = new ForesightConfig(
            c -> {
                Controller primaryTranslationalForward = Controller.proportional(0.26366644806863654);
                Controller secondaryTranslationalForward = Controller.proportional(0.09741770726411371);
                Controller primaryTranslationalLateral = Controller.proportional(0.3780695605552173);
                Controller secondaryTranslationalLateral = Controller.proportional(0.13968660042043982);

                c.forwardTranslational.set(Controller.piecewise(secondaryTranslationalForward).put(2.5, primaryTranslationalForward));
                c.strafeTranslational.set(Controller.piecewise(secondaryTranslationalLateral).put(2.5, primaryTranslationalLateral));

                c.coast.set(Controller.proportionalFeedforward(0.011231740481128884));
                c.brake.set(Controller.proportionalFeedforward(0.00954697940895955));

                c.headingFeedback.set(Controller.proportional(4.471166590600862));
                c.headingBrakeCoefficients.set(Vector2D.cartesian(0.05563932287355932, 0.006062764290200348));

                c.linearBrakeCoefficients.set(Matrix.diag(0.13199752251551877, 0.07306201166464231));
                c.quadraticBrakeCoefficients.set(Matrix.diag(0.0011244458886556464, 0.0016249793550451367));

                c.maxAchievableForwardVelocity.set(84.90549550715745);
                c.maxAchievableStrafeVelocity.set(68.98113543605689);
                c.naturalForwardDeceleration.set(26.567156797757455);
                c.naturalStrafeDeceleration.set(68.87885749501542);
            }
    );
    public static Follower create(HardwareMap h) {
        return new Follower(
            new PinpointLocalizer(h, localizerConfig),
            new Mecanum(h, drivetrainConfig),
            new Foresight(foresightConfig)
        );
    }
}