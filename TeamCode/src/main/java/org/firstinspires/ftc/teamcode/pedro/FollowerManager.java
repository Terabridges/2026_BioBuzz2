package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class FollowerManager {
    private static Follower instance = null;
    public static Follower getInstance(HardwareMap map) {
        if (instance == null) {
            instance = Constants.create(map);
        }
        return instance;
    }
}
