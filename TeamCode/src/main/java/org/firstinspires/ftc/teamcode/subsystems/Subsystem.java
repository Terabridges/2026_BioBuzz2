package org.firstinspires.ftc.teamcode.subsystems;

public interface Subsystem {
    void update();
    void toInit();
    void stop();

    default String getSubsystemName() {
        return getClass().getSimpleName();
    }
}