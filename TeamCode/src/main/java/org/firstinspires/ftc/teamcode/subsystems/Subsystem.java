package org.firstinspires.ftc.teamcode.subsystems;

public interface Subsystem {
    void toInit();
    void update();
    void stop();

    default String getSubsystemName() {
        return getClass().getSimpleName();
    }

    void logPsiKitData();
}